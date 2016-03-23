package com.strv.photomanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class ScaleImageHelper {

	private int mReqWidth;
	private int mReqHeight;
	private Context mContext;


	public ScaleImageHelper(Context context, int width, int height) {
		mReqWidth = width;
		mReqHeight = height;
		mContext = context;
	}


	public File scaleImageFile(File file) throws IOException {
		final String photoPath = file.getAbsolutePath();
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(photoPath, bmOptions);
		int inSampleSize = calculateInSampleSize(bmOptions, mReqWidth, mReqHeight);
		bmOptions.inSampleSize = inSampleSize;
		bmOptions.inJustDecodeBounds = false;

		Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
		double height = bitmap.getHeight();
		double width = bitmap.getWidth();
		if(width > height) {
			// landscape
			double ratio = width / mReqWidth;
			width = mReqWidth;
			height = height / ratio;
		} else if(height > width) {
			// portrait
			double ratio = height / mReqHeight;
			height = mReqHeight;
			width = width / ratio;
		} else {
			// square
			height = mReqHeight;
			width = mReqWidth;
		}

		bitmap.recycle();
		Bitmap scaledBitmap = checkOrientationAndSize(photoPath, (int) width, (int) height, mReqWidth, mReqHeight, inSampleSize);

		return writeCompressedBitmap(mContext, scaledBitmap);
	}


	private File writeCompressedBitmap(Context context, Bitmap bitmap) throws IOException {
		File cacheDir = context.getExternalCacheDir();
		if(cacheDir == null) {
			cacheDir = context.getCacheDir();
		}
		final File scaledFile = File.createTempFile("photo", ".jpg", cacheDir);
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(scaledFile));
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

		int size = (int) scaledFile.length() / 1024;

		int quality = 95;

		while(size > 320) {
			outputStream = new BufferedOutputStream(new FileOutputStream(scaledFile));
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
			size = (int) scaledFile.length() / 1024;
			quality -= 5;
		}

		return scaledFile;
	}


	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if(height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while((halfHeight / inSampleSize) > reqHeight || (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}


	private Bitmap checkOrientationAndSize(String path, int width, int height, int reqWidth, int reqHeight, int inSampleSize) {
		int orientation;
		try {
			if(path == null) {
				return null;
			}

			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = inSampleSize;
			bmOptions.inJustDecodeBounds = false;

			Bitmap bm = BitmapFactory.decodeFile(path, bmOptions);
			bm = Bitmap.createScaledBitmap(bm, width, height, true); // if the bitmap is too large this can cause out of memory
			Bitmap bitmap = bm;

			ExifInterface exif = new ExifInterface(path);

			orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

			Matrix m = new Matrix();

			if((orientation == ExifInterface.ORIENTATION_ROTATE_180)) {
				m.postRotate(180);
				bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
				return bitmap;
			} else if(orientation == ExifInterface.ORIENTATION_ROTATE_90) {
				m.postRotate(90);
				bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
				return bitmap;
			} else if(orientation == ExifInterface.ORIENTATION_ROTATE_270) {
				m.postRotate(270);

				bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
				return bitmap;
			}
			return bitmap;
		} catch(Exception e) {
			return null;
		}
	}

}
