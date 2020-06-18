// Copyright <2020> <The Nielsen Company>
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
// (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
// merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#import "NielsenNzme.h"
#import <NielsenAppApi/NielsenAppApi.h>

#define kCfgNmPlayerId @"nol_playerid"

static NSString *const TAG = @"NielsenAppApiBridge";

@interface NielsenNzme() <NielsenAppApiDelegate>

@property (nonatomic) NSMutableDictionary<NSString *, NielsenAppApi *> *nlsSDKs;

@end

@implementation NielsenNzme

RCT_EXPORT_MODULE()

// Example method
// See // https://facebook.github.io/react-native/docs/native-modules-ios
RCT_REMAP_METHOD(multiply,
                 multiplyWithA:(nonnull NSNumber*)a withB:(nonnull NSNumber*)b
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)
{
  NSNumber *result = @([a floatValue] * [b floatValue]);

  resolve(result);
}

/**
 * Since we need to communicate the opt-out url to javascript via events
 * we need to implement the following method
 */
- (NSArray<NSString *> *)supportedEvents
{
    return @[@"EVENT_INIT", @"EVENT_OPTOUT_URL", @"EVENT_OPTOUT_STATUS", @"EVENT_DEMOGRAPHIC_ID", @"EVENT_METER_VERSION"];
}

/**
* Creates SDK instance and passes on the provided metadata
* appInfo is simply passed to initWithAppInfo since the SDK already
* performs error checking
*/
RCT_EXPORT_METHOD(createInstance:(NSDictionary *)appInfo)
{
  NSLog(@"createInstance: %@", appInfo);

  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
      self.nlsSDKs = NSMutableDictionary.dictionary;
  });

  @synchronized (self) {
    NSMutableDictionary *bridgedInfo = [NSMutableDictionary dictionaryWithDictionary:appInfo];
    NSString *sdk_id = [NSString stringWithFormat:@"%lli", (long long)([[NSDate date] timeIntervalSince1970] * 1000)];

    bridgedInfo[kCfgNmPlayerId] = sdk_id;
    NielsenAppApi *nlsSDK = [[NielsenAppApi alloc] initWithAppInfo:bridgedInfo delegate:self];
    if (nlsSDK) {
      self.nlsSDKs[sdk_id] = nlsSDK;
      [self sendEventWithName:@"EVENT_INIT" body:@{@"id": sdk_id}];
    }
  }
}

/**
 * Wrapper for the SDK's play method. The provided channelInfo is
 * simply passed on
 */
RCT_EXPORT_METHOD(play:(NSString *)sdk_id :(NSDictionary *)channelInfo)
{
    NSLog(@"play: %@", channelInfo);

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] play:channelInfo];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
 * Wrapper for the SDK's loadMetadata method. The provided contentMetaData is
 * simply passed on
 */
RCT_EXPORT_METHOD(loadMetadata:(NSString *)sdk_id :(NSDictionary *)contentMetaData)
{
    NSLog(@"loadMetadata: %@", contentMetaData);

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] loadMetadata:contentMetaData];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
 * Wrapper for the SDK's stop method.
 */
RCT_EXPORT_METHOD(stop:(NSString *)sdk_id)
{
    NSLog( @"stop");

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] stop];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
 * Wrapper for the SDK's end method.
 */
RCT_EXPORT_METHOD(end:(NSString *)sdk_id)
{
    NSLog(@"end");

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] end];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
 * Wrapper for the SDK's playheadPosition method. The provided playhead is passed on to the SDK
 */
RCT_EXPORT_METHOD(setPlayheadPosition:(NSString *)sdk_id :(nonnull NSNumber *)ph)
{
    NSLog(@"setPlayheadPosition: %@",ph);

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] playheadPosition:[ph longLongValue]];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
 * Wrapper for the SDK's sendID3 method. The provided  string is passed on to the SDK
 */
RCT_EXPORT_METHOD(sendID3:(NSString *)sdk_id :(nonnull NSString *)id3)
{
    NSLog(@"sendID3: %@",id3);

    if (self.nlsSDKs[sdk_id]) {
        [self.nlsSDKs[sdk_id] sendID3:id3];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
* Destroying of SDK instance
*/
RCT_EXPORT_METHOD(free:(NSString *)sdk_id)
{
    NSLog(@"free: %@", sdk_id);

    if (self.nlsSDKs[sdk_id]) {
        self.nlsSDKs[sdk_id] = nil;
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
* Retrieves the demographic Id from the SDK instance and fires off the
* EVENT_DEMOGRAPHIC_ID event, so the meter version can be captured
*/
RCT_EXPORT_METHOD(getDemographicId:(NSString *)sdk_id)
{
    NSLog(@"getDemographicId: %@", sdk_id);

    if (self.nlsSDKs[sdk_id]) {
        [self sendEventWithName:@"EVENT_DEMOGRAPHIC_ID" body:@{@"demographic_id": self.nlsSDKs[sdk_id].demographicId}];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
* Retrieves the status from the SDK instance and fires off the
* EVENT_OPTOUT_STATUS event, so the status can be captured
*/
RCT_EXPORT_METHOD(getOptOutStatus:(NSString *)sdk_id)
{
    NSLog(@"getOptOutStatus: %@", sdk_id);

    if (self.nlsSDKs[sdk_id]) {
        [self sendEventWithName:@"EVENT_OPTOUT_STATUS" body:@{@"user_optout": [NSString stringWithFormat:@"%d", self.nlsSDKs[sdk_id].optOutStatus]}];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
* Retrieves the url from the SDK instance and fires off the
* EVENT_OPTOUT_URL event, so the url can be captured
*/
RCT_EXPORT_METHOD(userOptOutURLString:(NSString *)sdk_id)
{
    NSLog(@"userOptOutURLString: %@", sdk_id);

    if (self.nlsSDKs[sdk_id]) {
        [self sendEventWithName:@"EVENT_OPTOUT_URL" body:@{@"optouturl": self.nlsSDKs[sdk_id].optOutURL}];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

/**
* Retrieves the meter version from the SDK instance and fires off the
* EVENT_METER_VERSION event, so the meter version can be captured
*/
RCT_EXPORT_METHOD(getMeterVersion:(NSString *)sdk_id)
{
    NSLog(@"getMeterVersion: %@", sdk_id);

    if (self.nlsSDKs[sdk_id]) {
        [self sendEventWithName:@"EVENT_METER_VERSION" body:@{@"meter_version": self.nlsSDKs[sdk_id].meterVersion}];
    } else {
      NSLog(@"Error: instance %@ not found", sdk_id);
    }
}

- (void)dealloc
{
  self.nlsSDKs = nil;
}

@end
