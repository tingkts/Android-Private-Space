package com.ting.privatephoto.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;
import android.os.AsyncTask;
import com.ting.privatephoto.media.MediaItem;
import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;

    private BitmapCache mImageCache;

    private Resources mResources;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = false;

    private boolean mExitTasksEarly = false;
    private boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();

    protected ImageWorker(Context context) {
        mResources = context.getResources();
    }

    public void loadImage(MediaItem data, ImageView imageView, OnImageLoadedListener listener) {
        if (data == null) {
            return;
        }

        Bitmap value = null;

        synchronized(mImageCache) {
            if (mImageCache != null) {
                value = mImageCache.get(data.id);
            }
        }

        Log.d(TAG, "loadImage - " + data + ", " + imageView + ", " + value);

        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageBitmap(value);
            if (listener != null) {
                listener.onImageLoaded(true);
            }
        } else if (cancelPotentialWork(data, imageView)) {
            //BEGIN_INCLUDE(execute_background_task)
            final BitmapWorkerTask task = new BitmapWorkerTask(data, imageView, listener);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mResources, mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            // NOTE: This uses a custom version of AsyncTask that has been pulled from the
            // framework and slightly modified. Refer to the docs at the top of the class
            // for more info on what was changed.
            try {
                task.executeOnExecutor(THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {
                Log.d(TAG, "loadImage - " + data + ", " + e);
            }
            //END_INCLUDE(execute_background_task)
        }
    }

    public void loadImage(MediaItem data, ImageView imageView) {
        loadImage(data, imageView, null);
    }

    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    public void addImageCache(BitmapCache cache) {
        mImageCache = cache;
    }

    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    protected abstract Bitmap processBitmap(MediaItem data);

//    protected BitmapCache getImageCache() {
//        return mImageCache;
//    }

    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (Config.DEBUG) {
                final Object bitmapData = bitmapWorkerTask.mData;
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
            }
        }
    }

    private static boolean cancelPotentialWork(MediaItem data, ImageView imageView) {
        //BEGIN_INCLUDE(cancel_potential_work)
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mData;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                if (Config.DEBUG) {
                    Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
                }
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
        //END_INCLUDE(cancel_potential_work)
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable> {
        private MediaItem mData;
        private final WeakReference<ImageView> imageViewReference;
        private final OnImageLoadedListener mOnImageLoadedListener;

        public BitmapWorkerTask(MediaItem data, ImageView imageView) {
            mData = data;
            imageViewReference = new WeakReference<ImageView>(imageView);
            mOnImageLoadedListener = null;
        }

        BitmapWorkerTask(MediaItem data, ImageView imageView, OnImageLoadedListener listener) {
            mData = data;
            imageViewReference = new WeakReference<ImageView>(imageView);
            mOnImageLoadedListener = listener;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            //BEGIN_INCLUDE(load_bitmap_in_background)
            if (Config.DEBUG) {
                Log.d(TAG, "doInBackground - starting work, " + mData);
            }

            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            // Wait here if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {}
                }
            }

            // If the image cache is available and this task has not been cancelled by another
            // thread and the ImageView that was originally bound to this task is still bound back
            // to this task and our "exit early" flag is not set then try and fetch the bitmap from
            // the cache
            synchronized(mImageCache) {
                if (mImageCache != null && !isCancelled() && getAttachedImageView() != null
                        && !mExitTasksEarly) {
                    bitmap = mImageCache.get(mData.id);
                }
            }

            // If the bitmap was not found in the cache and this task has not been cancelled by
            // another thread and the ImageView that was originally bound to this task is still
            // bound back to this task and our "exit early" flag is not set, then call the main
            // process method (as implemented by a subclass)
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null
                    && !mExitTasksEarly) {
                int retryCount = 0;
                do {
                    bitmap = processBitmap(mData);
                    if (bitmap != null) {
                        break;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "doInBackground - do work, sleep interrupted, retryCount="
                                    + retryCount + ", " + mData + ", " + bitmap);
                        }
                    }
                    retryCount++;
                    Log.d(TAG, "doInBackground - do work, retryCount=" + retryCount
                            + ", " + mData + ", " + bitmap);
                } while (retryCount < 3);
            }

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the cache for future use. Note we don't check if the task was cancelled
            // here, if it was, and the thread is still running, we may as well add the processed
            // bitmap to our cache as it might be used again in the future
            if (bitmap != null) {
                drawable = new BitmapDrawable(mResources, bitmap);

                synchronized(mImageCache) {
                    if (mImageCache != null) {
                        mImageCache.put(mData.id, bitmap);
                    }
                }
            }

            if (Config.DEBUG) {
                Log.d(TAG, "doInBackground - finished work, " + mData + ", " + bitmap);
            }

            return drawable;
            //END_INCLUDE(load_bitmap_in_background)
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            //BEGIN_INCLUDE(complete_background_work)
            boolean success = false;
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled() || mExitTasksEarly) {
                value = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (value != null && imageView != null) {
                if (Config.DEBUG) {
                    Log.d(TAG, "onPostExecute - setting bitmap, " + mData + ", " + imageView);
                }
                success = true;
                setImageDrawable(imageView, value);
            }
            if (mOnImageLoadedListener != null) {
                mOnImageLoadedListener.onImageLoaded(success);
            }
            //END_INCLUDE(complete_background_work)
        }

        @Override
        protected void onCancelled(BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    /**
     * Interface definition for callback on image loaded successfully.
     */
    public interface OnImageLoadedListener {

        /**
         * Called once the image has been loaded.
         * @param success True if the image was loaded successfully, false if
         *                there was an error.
         */
        void onImageLoaded(boolean success);
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work is in progress.
     * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can bind its result,
     * independently of the finish order.
     */
    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        if (mFadeInBitmap) {
            // Transition drawable with a transparent drawable and the final drawable
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            new ColorDrawable(android.R.color.transparent),
                            drawable
                    });
            // Set background to loading bitmap
            imageView.setBackgroundDrawable(
                    new BitmapDrawable(mResources, mLoadingBitmap));

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }

    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    public boolean removeImage(long id) {
        synchronized(mImageCache) {
            return (mImageCache.remove(id) != null);
        }
    }

    public Bitmap getImage(long id) {
        synchronized(mImageCache) {
            return mImageCache.get(id);
        }
    }
}