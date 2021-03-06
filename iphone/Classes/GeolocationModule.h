/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiModule.h"
#import <CoreLocation/CoreLocation.h>

@interface GeolocationModule : TiModule<CLLocationManagerDelegate> {
	CLLocationManager *locationManager;
	CLLocationAccuracy accuracy;
	CLLocationDistance distance;
	CLLocationDegrees heading;
	BOOL calibration;
	NSMutableArray *singleHeading;
	NSMutableArray *singleLocation;
}

@property(nonatomic,readonly,getter=hasCompass) NSNumber *compass;
@property(nonatomic,readwrite,assign) NSNumber *accuracy;
@property(nonatomic,readwrite,assign) NSNumber *highAccuracy;
@property(nonatomic,readwrite,assign) NSNumber *showCalibration;
@property(nonatomic,readwrite,assign) NSNumber *distanceFilter;
@property(nonatomic,readwrite,assign) NSNumber *headingFilter;
@property(nonatomic,readonly) NSNumber *locationServicesEnabled;


@property(nonatomic,readonly) NSNumber *ACCURACY_BEST;
@property(nonatomic,readonly) NSNumber *ACCURACY_NEAREST_TEN_METERS;
@property(nonatomic,readonly) NSNumber *ACCURACY_HUNDRED_METERS;
@property(nonatomic,readonly) NSNumber *ACCURACY_KILOMETER;
@property(nonatomic,readonly) NSNumber *ACCURACY_THREE_KILOMETERS;


@end
