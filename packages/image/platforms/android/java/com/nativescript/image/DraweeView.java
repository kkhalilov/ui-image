package com.nativescript.image;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchy;

import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View.MeasureSpec;
import android.view.View;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.AttributeSet;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.ViewOutlineProvider;

import org.nativescript.widgets.BorderDrawable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Arrays;

public class DraweeView extends SimpleDraweeView {
    public int imageWidth = 0;
    public int imageHeight = 0;
    private static Paint clipPaint;

    private boolean clipEnabled = true;

    public DraweeView(Context context, GenericDraweeHierarchy hierarchy) {
    super(context);
    setClipToBounds(clipEnabled);
  }

  public DraweeView(Context context) {
    super(context);
    setClipToBounds(clipEnabled);
  }

  public DraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setClipToBounds(clipEnabled);
  }

  public DraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setClipToBounds(clipEnabled);
  }

    public void setClipToBounds(boolean value) {
        clipEnabled = value;
        if (value) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        Drawable drawable = getBackground();
                        if (drawable instanceof BorderDrawable) {
                            BorderDrawable borderDrawable = (BorderDrawable) drawable;
                            float borderTopLeftRadius = borderDrawable.getBorderTopLeftRadius();
                            float borderTopRightRadius = borderDrawable.getBorderTopRightRadius();
                            float borderBottomRightRadius = borderDrawable.getBorderBottomRightRadius();
                            float borderBottomLeftRadius = borderDrawable.getBorderBottomLeftRadius();
                            float borderLeftWidth = borderDrawable.getBorderLeftWidth();
                            float borderBottomWidth = borderDrawable.getBorderBottomWidth();
                            float borderTopWidth = borderDrawable.getBorderTopWidth();
                            float borderRightWidth = borderDrawable.getBorderRightWidth();
                            if (android.os.Build.VERSION.SDK_INT >= 21 && 
                                borderDrawable.getUniformBorderRadius() > 0 &&
                                borderLeftWidth == 0 &&
                                borderBottomWidth == 0 &&
                                borderTopWidth == 0 &&
                                borderRightWidth == 0) {
                                drawable.getOutline(outline);
                                return;
                            }
                        }
                        outline.setRect(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                setClipToOutline(true);
            }
        } else if (android.os.Build.VERSION.SDK_INT >= 21) {
            setOutlineProvider(null);
            setClipToOutline(false);
        }
    }

    public boolean getClipToBounds() {
        return clipEnabled;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        final float aspectRatio = this.getAspectRatio();
        if (aspectRatio > 0) {
            boolean finiteWidth = widthMode == android.view.View.MeasureSpec.EXACTLY;
            boolean finiteHeight = heightMode == android.view.View.MeasureSpec.EXACTLY;
            if (imageWidth != 0 && imageHeight != 0) {
                if (!finiteWidth) {
                    widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec((int) (height * aspectRatio),
                            android.view.View.MeasureSpec.EXACTLY);
                }
                if (!finiteHeight) {
                    heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec((int) (width / aspectRatio),
                            android.view.View.MeasureSpec.EXACTLY);
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    Path innerBorderPath;
    Path innerBorderTempPath;

    private Path generateInnerBorderPath(BorderDrawable borderDrawable) {

        float borderTopLeftRadius = borderDrawable.getBorderTopLeftRadius();
        float borderTopRightRadius = borderDrawable.getBorderTopRightRadius();
        float borderBottomRightRadius = borderDrawable.getBorderBottomRightRadius();
        float borderBottomLeftRadius = borderDrawable.getBorderBottomLeftRadius();
        float borderLeftWidth = borderDrawable.getBorderLeftWidth();
        float borderBottomWidth = borderDrawable.getBorderBottomWidth();
        float borderTopWidth = borderDrawable.getBorderTopWidth();
        float borderRightWidth = borderDrawable.getBorderRightWidth();
        if (android.os.Build.VERSION.SDK_INT >= 21 && ((borderDrawable.hasUniformBorderRadius()
                && (borderLeftWidth == 0 && borderBottomWidth == 0 && borderTopWidth == 0 && borderRightWidth == 0))
                || (borderTopLeftRadius == 0 && borderTopRightRadius == 0 && borderBottomRightRadius == 0
                        && borderBottomLeftRadius == 0))) {
                // we can use outline
            return null;
        }
        if (innerBorderPath == null) {
            innerBorderPath = new Path();
        } else {
            innerBorderPath.reset();
        }
        if (innerBorderTempPath == null) {
            innerBorderTempPath = new Path();
        } else {
            innerBorderTempPath.reset();
        }
        Rect bounds = borderDrawable.getBounds();
        float width = (float) borderDrawable.getBounds().width();
        float height = (float) borderDrawable.getBounds().height();

        RectF borderInnerRect = new RectF(borderLeftWidth, borderTopWidth, width - borderRightWidth,
                height - borderBottomWidth);
        float[] borderInnerRadii = { Math.max(0, borderTopLeftRadius - borderLeftWidth),
                Math.max(0, borderTopLeftRadius - borderTopWidth), Math.max(0, borderTopRightRadius - borderRightWidth),
                Math.max(0, borderTopRightRadius - borderTopWidth),
                Math.max(0, borderBottomRightRadius - borderRightWidth),
                Math.max(0, borderBottomRightRadius - borderBottomWidth),
                Math.max(0, borderBottomLeftRadius - borderLeftWidth),
                Math.max(0, borderBottomLeftRadius - borderBottomWidth) };
        innerBorderTempPath.addRoundRect(borderInnerRect, borderInnerRadii, Path.Direction.CW);
        innerBorderPath.addRect(new RectF(bounds), Path.Direction.CW);
        innerBorderPath.op(innerBorderTempPath, Path.Op.DIFFERENCE);
        return innerBorderPath;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getBackground();
        if (clipEnabled && drawable instanceof BorderDrawable) {
            BorderDrawable borderDrawable = (BorderDrawable) drawable;
            Path clipPath = generateInnerBorderPath(borderDrawable);
            if (clipPath != null) {
                if (DraweeView.clipPaint == null) {
                    DraweeView.clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    DraweeView.clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }
                int saveCount;
                int width = getWidth();
                int height = getHeight();
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    saveCount = canvas.saveLayer(new android.graphics.RectF(0.0f, 0.0f, width, height), null);
                } else {
                    saveCount = canvas.saveLayer(0.0f, 0.0f, width, height, null, Canvas.ALL_SAVE_FLAG);
                }
                super.onDraw(canvas);
                canvas.drawPath(clipPath, DraweeView.clipPaint);
                canvas.restoreToCount(saveCount);
                return;
            }
        }
        super.onDraw(canvas);
    }

    public void setUri(android.net.Uri uri, String jsonOptions, com.facebook.drawee.controller.ControllerListener listener) {
        Log.d("JS", "setUri " + uri.toString());
    ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri).setRotationOptions( com.facebook.imagepipeline.common.RotationOptions.autoRotate());
        JSONObject object = null;
        if (jsonOptions.length() > 2) {
            try {
                object = new JSONObject(jsonOptions);
            } catch (Exception ignored) {
            }
        }
        if (object != null) {
            if (object.optBoolean("progressiveRenderingEnabled")) {
                requestBuilder = requestBuilder.setProgressiveRenderingEnabled(true);
            }
            if (object.optBoolean("localThumbnailPreviewsEnabled")) {
                requestBuilder = requestBuilder.setLocalThumbnailPreviewsEnabled(true);
            }
            int decodeWidth = object.optInt("decodeWidth");
            int decodeHeight = object.optInt("decodeHeight");

            if (decodeWidth > 0 && decodeHeight > 0) {
                requestBuilder = requestBuilder.setResizeOptions(new com.facebook.imagepipeline.common.ResizeOptions(decodeWidth, decodeHeight));
            }
            int blurRadius = object.optInt("blurRadius", 0);
            if (blurRadius > 0) {
                int blurDownSampling = object.optInt("blurDownSampling", 1);
                requestBuilder = requestBuilder.setPostprocessor(new com.nativescript.image.ScalingBlurPostprocessor(2, blurRadius, blurDownSampling));
            }
        }
        
        ImageRequest request = requestBuilder.build();
        PipelineDraweeControllerBuilder builder = com.facebook.drawee.backends.pipeline.Fresco.newDraweeControllerBuilder();
        builder.setImageRequest(request);
        builder.setCallerContext(uri.toString());
        builder.setControllerListener(listener);
        builder.setOldController(getController());
        // if (Trace.isEnabled()) {
        //     builder.setPerfDataListener(
        //         new com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener({
        //             onImageLoadStatusUpdated(param0: com.facebook.drawee.backends.pipeline.info.ImagePerfData, param1: number) {
        //                 CLog(CLogTypes.info, 'onImageLoadStatusUpdated', param0, param1);
        //             },
        //             onImageVisibilityUpdated(param0: com.facebook.drawee.backends.pipeline.info.ImagePerfData, param1: number) {
        //                 CLog(CLogTypes.info, 'onImageVisibilityUpdated', param0, param1);
        //             }
        //         })
        //     );
        // }
        if (object != null) {
           String lowerResSrc = object.optString("lowerResSrc");
            if (lowerResSrc != null) {
                builder.setLowResImageRequest(com.facebook.imagepipeline.request.ImageRequest.fromUri(android.net.Uri.parse(lowerResSrc)));
            }

            if (object.optBoolean("autoPlayAnimations")) {
                builder.setAutoPlayAnimations(true);
            }

            if (object.optBoolean("tapToRetryEnabled")) {
                builder.setTapToRetryEnabled(true);
            }
        }
        Log.d("JS", "setUri " + uri.toString() + " " + object);
        setController(builder.build());
    }
}
