#import "ScreenRecorder.h"
#import <React/RCTConvert.h>

@implementation ScreenRecorder

- (void) muteAudioInBuffer:(CMSampleBufferRef)sampleBuffer
{
    CMItemCount numSamples = CMSampleBufferGetNumSamples(sampleBuffer);
    NSUInteger channelIndex = 0;

    CMBlockBufferRef audioBlockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer);
    size_t audioBlockBufferOffset = (channelIndex * numSamples * sizeof(SInt16));
    size_t lengthAtOffset = 0;
    size_t totalLength = 0;
    SInt16 *samples = NULL;
    CMBlockBufferGetDataPointer(audioBlockBuffer, audioBlockBufferOffset, &lengthAtOffset, &totalLength, (char **)(&samples));

    for (NSInteger i=0; i<numSamples; i++) {
        samples[i] = (SInt16)0;
    }
}

// H264 bug if the frame size is not a multiple of 2
- (int) adjustMultipleOf2:(int)value;
{
    if (value % 2 == 1) {
        return value + 1;
    }
    return value;
}

- (NSString *) setupRecorder;
{
    if (@available(iOS 11.0, *)) {
        // Audio Setup
        AudioChannelLayout acl = { 0 };
        acl.mChannelLayoutTag = kAudioChannelLayoutTag_Stereo;
        NSDictionary *audioSettings = @{
            AVFormatIDKey: @(kAudioFormatMPEG4AAC),
            AVSampleRateKey: @(44100),
            AVChannelLayoutKey: [NSData dataWithBytes: &acl length: sizeof( acl ) ],
            AVEncoderBitRateKey: @(64000)};

        self.audioInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeAudio outputSettings:audioSettings];
        self.audioInput.preferredVolume = 0.0;

        self.micInput = [[AVAssetWriterInput alloc] initWithMediaType:AVMediaTypeAudio outputSettings:audioSettings];
        self.micInput.preferredVolume = 0.0;
        
        // Video Setup
        NSDictionary *compressionProperties = @{
            AVVideoProfileLevelKey         : AVVideoProfileLevelH264HighAutoLevel,
            AVVideoH264EntropyModeKey      : AVVideoH264EntropyModeCABAC,
            AVVideoAverageBitRateKey       : @(1920 * 1080 * 114),
            AVVideoMaxKeyFrameIntervalKey  : @60,
            AVVideoAllowFrameReorderingKey : @NO};

        NSDictionary *videoSettings = @{
            AVVideoCompressionPropertiesKey : compressionProperties,
            AVVideoCodecKey                 : AVVideoCodecTypeH264,
            AVVideoWidthKey                 : @([self adjustMultipleOf2:self.screenWidth]),
            AVVideoHeightKey                : @([self adjustMultipleOf2:self.screenHeight])};

        self.videoInput = [AVAssetWriterInput assetWriterInputWithMediaType:AVMediaTypeVideo outputSettings:videoSettings];

        // Adds inputs to writer
        [self.writer addInput:self.micInput];
        [self.writer addInput:self.audioInput];
        [self.writer addInput:self.videoInput];

        // Setup video
        [self.videoInput setMediaTimeScale:60];
        [self.writer setMovieTimeScale:60];
        [self.videoInput setExpectsMediaDataInRealTime:YES];
        self.encounteredFirstBuffer = NO;

        // Setup mic
        if (self.enableMic) {
            self.screenRecorder.microphoneEnabled = YES;
        }
        return nil;
    } else {
        return @"iOS version too low";
    }
}

RCT_EXPORT_MODULE()


RCT_REMAP_METHOD(startRecording, config:(NSDictionary *)config resolve:(RCTPromiseResolveBlock)resolve rejecte:(RCTPromiseRejectBlock)reject)
{
    // Checks if already recording screen
    self.screenRecorder = [RPScreenRecorder sharedRecorder];
    if (self.screenRecorder.isRecording) {
        return;
    }
    
    // Configures screen and microphone
    self.screenWidth = [self adjustMultipleOf2:[RCTConvert int: config[@"width"]]];
    self.screenHeight = [self adjustMultipleOf2:[RCTConvert int: config[@"height"]]];
    self.enableMic = [RCTConvert BOOL: config[@"mic"]];
    NSLog(@"ScreenRecorder: width: %d", self.screenWidth);
    NSLog(@"ScreenRecorder: height: %d", self.screenHeight);
    
    // Generates video Filename using epoch
    NSArray *pathDocuments = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *videoOutPath = [[pathDocuments[0] stringByAppendingPathComponent:
        [NSString stringWithFormat:@"%f", [[NSDate date] timeIntervalSince1970]]]
        stringByAppendingPathExtension:@"mp4"];
    NSLog(@"ScreenRecorder: Video file: %@", videoOutPath);

    // Initializes writer
    NSError *error;
    self.writer = [AVAssetWriter assetWriterWithURL:[NSURL fileURLWithPath:videoOutPath] fileType:AVFileTypeMPEG4 error:&error];
    if (!self.writer) {
        NSLog(@"ScreenRecorder: Writer init error: %@", error);
        reject(0, @"Writer init error", nil);
        return;
    }
    
    // Setup Audio and Video streams
    NSString *initRecorderError = [self setupRecorder];
    if (initRecorderError != nil){
        NSLog(@"ScreenRecorder: Initialization error %@", initRecorderError);
        reject(0, [NSString stringWithFormat:@"Initialization error: %@", initRecorderError], nil);
        return;
    }

        
    if (@available(iOS 11.0, *)) {
        // Starts screen recording
        [self.screenRecorder
         
            startCaptureWithHandler: ^(CMSampleBufferRef sampleBuffer, RPSampleBufferType bufferType, NSError* error) {
                [self captureSampleBuffer:sampleBuffer withBufferType:bufferType];
            }

            completionHandler: ^(NSError* error) {
                NSLog(@"ScreenRecorder: startCapture completionHandler %@", error);
                if (error) {
                    resolve(@"userDeniedPermission");
                } else {
                    resolve(@"started");
                }
            }

        ];
    } else {
        reject(0, @"iOS Version", nil);
    }
}

// Receives a sample buffer from ReplayKit every frame.
-(void)captureSampleBuffer:(CMSampleBufferRef)sampleBuffer withBufferType:(RPSampleBufferType)bufferType
{
    if(CMSampleBufferDataIsReady(sampleBuffer) == false || self.writer == nil) {
        NSLog(@"ScreenRecorder: sample False or no assetWriter");
        return;
    }

    if (self.writer.status == AVAssetWriterStatusFailed) {
        NSLog(@"ScreenRecorder: AVAssetWriterStatusFailed");
        return;
    }

    // Uses a queue in sync so that the writer-starting logic won't be invoked twice.
    dispatch_sync(dispatch_get_main_queue(), ^{
        if (!CMSampleBufferDataIsReady(sampleBuffer)) {
            NSLog(@"ScreenRecorder: !CMSampleBufferDataIsReady");
            return;
        }

        if (self.writer.status == AVAssetWriterStatusUnknown && !self.encounteredFirstBuffer && bufferType == RPSampleBufferTypeVideo) {
            NSLog(@"ScreenRecorder: encounteredFirstBuffer");
            self.encounteredFirstBuffer = YES;
            [self.writer startWriting];
            [self.writer startSessionAtSourceTime:CMSampleBufferGetPresentationTimeStamp(sampleBuffer)];
            return;
        }
                            
        if (self.writer.status == AVAssetWriterStatusWriting) {
            switch (bufferType) {
                case RPSampleBufferTypeVideo:
                    if (self.videoInput.isReadyForMoreMediaData) {
                        @try {
                            [self.videoInput appendSampleBuffer:sampleBuffer];
                        } @catch(NSException *expection) {
                            NSLog(@"Missed Video Buffer: %@", self.writer.error);
                        }
                    }
                    break;




                case RPSampleBufferTypeAudioApp:
                    @try {
                        if (self.audioInput.isReadyForMoreMediaData) {
                            if(self.enableMic){
                                [self.audioInput appendSampleBuffer:sampleBuffer];
                            } else {
                                [self muteAudioInBuffer:sampleBuffer];
                            }
                        }
                    } @catch(NSException *expection) {
                            NSLog(@"Missed App Audio Buffer: %@", self.writer.error);
                    }
                    break;
                



                case RPSampleBufferTypeAudioMic:
                    if (self.micInput.isReadyForMoreMediaData) {
                        @try {
                            if(self.enableMic){
                                [self.micInput appendSampleBuffer:sampleBuffer];
                            } else {
                                [self muteAudioInBuffer:sampleBuffer];
                            }
                            } @catch(NSException *expection) {
                            NSLog(@"Missed Mic Audio Buffer: %@", self.writer.error);
                        }
                    }
                    break;

                default:
                    break;
            }
        }              
    });
}

RCT_REMAP_METHOD(stopRecording, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 11.0, *)) {
            [[RPScreenRecorder sharedRecorder] stopCaptureWithHandler:^(NSError * _Nullable error) {
                if (!error) {
                    [self.audioInput markAsFinished];
                    [self.micInput markAsFinished];
                    [self.videoInput markAsFinished];
                    [self.writer finishWritingWithCompletionHandler:^{
                    
                        resolve(self.writer.outputURL.absoluteString);
                        NSLog(@"ScreenRecorder: Recording stopped successfully. uri: %@", self.writer.outputURL.absoluteString);

                        self.audioInput = nil;
                        self.micInput = nil;
                        self.videoInput = nil;
                        self.writer = nil;
                        self.screenRecorder = nil;
                    
                    }];
                }
                else {
                    NSLog(@"ScreenRecorder: stopCaptureWithHandler %@", error);
                    reject(0, @"stopCaptureWithHandler", error);
                }
            }];
        } else {
            reject(0, @"iOS Version", nil);
        }
    });
}

RCT_EXPORT_METHOD(deleteRecording: (NSString *)fileToDelete)
{
    @try {
        NSURL *fileUrl = [NSURL URLWithString:fileToDelete];
        NSLog(@"ScreenRecorder: About to delete : %@", fileToDelete);
        [[NSFileManager defaultManager] removeItemAtPath:[fileUrl path] error:nil];
    } @catch(NSException *exception) {
        NSLog(@"ScreenRecorder: Errior deleting file: %@", exception);
    }
}

@end
