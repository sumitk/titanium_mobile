/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIScrollView.h"
#import "TiUIScrollViewProxy.h"
#import "TiUtils.h"

@implementation TiUIScrollView
@synthesize verticalLayoutBoundary;

- (void) dealloc
{
	RELEASE_TO_NIL(wrapperView);
	RELEASE_TO_NIL(scrollView);
	[super dealloc];
}

-(UIView *)wrapperView
{
	if (wrapperView == nil)
	{
		CGRect wrapperFrame;
		wrapperFrame.size = [[self scrollView] contentSize];
		wrapperFrame.origin = CGPointZero;
		wrapperView = [[UIView alloc] initWithFrame:wrapperFrame];
		[wrapperView setUserInteractionEnabled:YES];
		[scrollView addSubview:wrapperView];
	}
	return wrapperView;
}

-(UIScrollView *)scrollView
{
	if(scrollView == nil)
	{
		scrollView = [[UIScrollView alloc] initWithFrame:[self bounds]];
		[scrollView setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
		[scrollView setBackgroundColor:[UIColor clearColor]];
		[scrollView setShowsHorizontalScrollIndicator:NO];
		[scrollView setShowsVerticalScrollIndicator:NO];
		[scrollView setDelegate:self];
		[self addSubview:scrollView];
	}
	return scrollView;
}

-(void)setNeedsHandleContentSizeIfAutosizing
{
	if (TiDimensionIsAuto(contentWidth) || TiDimensionIsAuto(contentHeight))
	{
		[self setNeedsHandleContentSize];
	}
}

-(void)setNeedsHandleContentSize
{
	if (!needsHandleContentSize)
	{
		needsHandleContentSize = YES;
		[self performSelectorOnMainThread:@selector(handleContentSize) withObject:nil waitUntilDone:NO];
	}
}


-(BOOL)handleContentSizeIfNeeded
{
	if (needsHandleContentSize)
	{
		[self handleContentSize];
		return YES;
	}
	return NO;
}

-(void)handleContentSize
{
	CGSize newContentSize = [self bounds].size;
	
//	NSArray * subViews = [[self wrapperView] subviews];
	
	switch (contentWidth.type)
	{
		case TiDimensionTypePixels:
		{
			newContentSize.width = MAX(newContentSize.width,contentWidth.value);
			break;
		}
		case TiDimensionTypeAuto:
		{
			for (TiViewProxy * thisChildProxy in [(TiViewProxy *)[self proxy] children])
			{
				newContentSize.width = MAX(newContentSize.width,[thisChildProxy minimumParentWidthForWidth:newContentSize.width]);
			}
			break;
		}
	}

	switch (contentHeight.type)
	{
		case TiDimensionTypePixels:
		{
			minimumContentHeight = contentHeight.value;
			break;
		}
		case TiDimensionTypeAuto:
		{
			BOOL isVertical = TiLayoutRuleIsVertical([(TiViewProxy *)[self proxy] layoutProperties]->layout);
			minimumContentHeight=0.0;

			for (TiViewProxy * thisChildProxy in [(TiViewProxy *)[self proxy] children])
			{
				CGFloat thisHeight = [thisChildProxy minimumParentHeightForWidth:newContentSize.width];
				if (isVertical)
				{
					minimumContentHeight += thisHeight;
				}
				else if(minimumContentHeight<thisHeight)
				{
					minimumContentHeight = thisHeight;
				}
			}
			break;
		}
		default:
			minimumContentHeight = newContentSize.height;
			break;
	}
	newContentSize.height = MAX(newContentSize.height,minimumContentHeight);

	[scrollView setContentSize:newContentSize];
	CGRect wrapperBounds;
	wrapperBounds.origin = CGPointZero;
	wrapperBounds.size = newContentSize;
	[wrapperView setBounds:wrapperBounds];
	[wrapperView setCenter:CGPointMake(newContentSize.width/2, newContentSize.height/2)];
	needsHandleContentSize = NO;
	[(TiViewProxy *)[self proxy] layoutChildren];
}

-(void)setContentWidth_:(id)value
{
	contentWidth = [TiUtils dimensionValue:value];
	[self setNeedsHandleContentSize];
}

-(void)setContentHeight_:(id)value
{
	contentHeight = [TiUtils dimensionValue:value];
	[self setNeedsHandleContentSize];
}

-(void)setShowHorizontalScrollIndicator_:(id)value
{
	[[self scrollView] setShowsHorizontalScrollIndicator:[TiUtils boolValue:value]];
}

-(void)setShowVerticalScrollIndicator_:(id)value
{
	[[self scrollView] setShowsVerticalScrollIndicator:[TiUtils boolValue:value]];
}

-(void)setScrollIndicatorStyle_:(id)value
{
	[[self scrollView] setIndicatorStyle:[TiUtils intValue:value def:UIScrollViewIndicatorStyleDefault]];
}

-(void)setDisableBounce_:(id)value
{
	[[self scrollView] setBounces:![TiUtils boolValue:value]];
}

-(void)setHorizontalBounce_:(id)value
{
	[[self scrollView] setAlwaysBounceHorizontal:[TiUtils boolValue:value]];
}

-(void)setVerticalBounce_:(id)value
{
	[[self scrollView] setAlwaysBounceVertical:[TiUtils boolValue:value]];
}

-(void)setContentOffset_:(id)value
{
	CGPoint newOffset = [TiUtils pointValue:value];
	BOOL animated = scrollView != nil;
	[[self scrollView] setContentOffset:newOffset animated:animated];
}

-(void)setZoomScale_:(id)args
{
	CGFloat scale = [TiUtils floatValue:args];
	[[self scrollView] setZoomScale:scale];
	if ([self.proxy _hasListeners:@"scale"])
	{
		[self.proxy fireEvent:@"scale" withObject:[NSDictionary dictionaryWithObjectsAndKeys:
											NUMFLOAT(scale),@"scale",
											nil]];
	}
}

-(void)setMaxZoomScale_:(id)args
{
	[[self scrollView] setMaximumZoomScale:[TiUtils floatValue:args]];
}

-(void)setMinZoomScale_:(id)args
{
	[[self scrollView] setMinimumZoomScale:[TiUtils floatValue:args]];
}


#pragma mark scrollView delegate stuff


- (void)scrollViewDidScroll:(UIScrollView *)scrollView_               // any offset changes
{
	[(id<UIScrollViewDelegate>)[self proxy] scrollViewDidScroll:scrollView_];
}

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView
{
	return [self wrapperView];
}

- (void)scrollViewDidEndZooming:(UIScrollView *)scrollView_ withView:(UIView *)view atScale:(float)scale 
{
	// scale between minimum and maximum. called after any 'bounce' animations
	[(id<UIScrollViewDelegate>)[self proxy] scrollViewDidEndZooming:scrollView withView:(UIView*)view atScale:scale];
}

/*
- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView;                              // called on start of dragging (may require some time and or distance to move)
- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate; // called on finger up if user dragged. decelerate is true if it will continue moving afterwards

- (void)scrollViewWillBeginDecelerating:(UIScrollView *)scrollView;   // called on finger up as we are moving
- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView;      // called when scroll view grinds to a halt

- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView; // called when setContentOffset/scrollRectVisible:animated: finishes. not called if not animating

 - (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView;   // return a yes if you want to scroll to the top. if not defined, assumes YES
- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView;      // called when scrolling animation finished. may be called immediately if already at top
*/



-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop forView:(TiUIView *)firstResponderView
{
	lastFocusedView = firstResponderView;
	CGRect responderRect = [wrapperView convertRect:[firstResponderView bounds] fromView:firstResponderView];
	
	ModifyScrollViewForKeyboardHeightAndContentHeightWithResponderRect(scrollView,keyboardTop,minimumContentHeight,responderRect);
}

-(void)keyboardDidHideForView:(TiUIView *)hidingView
{
	if(hidingView != lastFocusedView)
	{
		return;
	}

	RestoreScrollViewFromKeyboard(scrollView);
}

@end
