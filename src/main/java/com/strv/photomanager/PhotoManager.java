package com.strv.photomanager;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;

import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.FlashMode;
import com.commonsware.cwac.cam2.VideoRecorderActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class PhotoManager {

	public static final int REQUEST_IMAGE_CAPTURE = 168;
	public static final int REQUEST_VIDEO_CAPTURE = 170;
	public static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 169;
	public static final int REQUEST_PHOTO_VIDEO_CAPTURE = 171;
	private static final long MAX_LENGTH = 15;
	private static final int DEFAULT_VIDEO_LENGTH = 15000;


	public interface OnFileFromUriExtractedListener {
		void onFileFromUriExtracted(File file, boolean isTrimmed);
	}


	/**
	 * creates a camera chooser for camera with all the apps on the device that support taking pictures
	 *
	 * @param outputFileUri      Uri where the captured image should be saved
	 * @param cameraChooserTitle title of the camera app chooser
	 * @return Intent that when run will initiate an app chooser
	 */
	private static Intent makeCameraChooserIntent(Uri outputFileUri, String cameraChooserTitle) {
		return Intent.createChooser(makeCameraIntent(outputFileUri), cameraChooserTitle);
	}


	/**
	 * creates a camera chooser for custom camera (cwac-cam2)
	 *
	 * @param outputFileUri      Uri where the captured image should be saved
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param context context of the app/activity
	 * @return Intent that when run will initiate an app chooser
	 */
	private static Intent makeCustomCameraChooserIntent(Uri outputFileUri, String cameraChooserTitle, Context context) {
		return Intent.createChooser(getLabeledIntentForPhotoCamera(context, makeCustomCameraIntent(outputFileUri, context)), cameraChooserTitle);
	}


	/**
	 * creates a camera chooser for camera with all the apps on the device that support taking pictures
	 *
	 * @param outputFileUri      Uri where the captured image should be saved
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Intent that when run will initiate an app chooser
	 */
	private static Intent makeVideoCameraChooserIntent(Uri outputFileUri, String cameraChooserTitle, int videoDurationLimit) {
		return Intent.createChooser(makeVideoCameraIntent(outputFileUri, videoDurationLimit), cameraChooserTitle);
	}


	/**
	 * creates an Intent to capture an image with external app
	 *
	 * @param outputFileUri Uri of the file where the captured image is supposed to be stored
	 * @return Intent that when run will initiate a camera app
	 */
	private static Intent makeCameraIntent(Uri outputFileUri) {
		Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri); // set the image file name
		return captureIntent;
	}


	/**
	 * creates an Intent to capture an image with custom camera (cwac-cam2)
	 *
	 * @param outputFileUri Uri of the file where the captured image is supposed to be stored
	 * @return Intent that when run will initiate a camera app
	 */
	private static Intent makeCustomCameraIntent(Uri outputFileUri, Context context) {
		CameraActivity.IntentBuilder intentBuilder = new CameraActivity.IntentBuilder(context);

		intentBuilder.to(outputFileUri);
		intentBuilder.flashMode(FlashMode.AUTO);

		Intent result = intentBuilder.build();

		return result;
	}


	private static Intent getLabeledIntentForPhotoCamera(Context context, Intent intent) {
		List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
		String packageName = "";
		if(resInfo != null && resInfo.isEmpty()) {
			packageName = resInfo.get(0).activityInfo.packageName;
		}
		return new LabeledIntent(intent, packageName, R.string.camera, R.drawable.camera_icon);
	}


	/**
	 * creates an Intent to capture a video with external app
	 *
	 * @param outputFileUri      Uri of the file where the captured image is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Intent that when run will initiate a camera app
	 */
	private static Intent makeVideoCameraIntent(Uri outputFileUri, int videoDurationLimit) {
		Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri); // set the image file name
		if(videoDurationLimit > 0) {
			captureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, videoDurationLimit); // set the image file name
		}
		return captureIntent;
	}


	/**
	 * creates an Intent to capture a video with custom camera(cwac-cam2)
	 *
	 * @param outputFileUri      Uri of the file where the captured image is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Intent that when run will initiate a camera app
	 */
	private static Intent makeCustomVideoCameraIntent(Uri outputFileUri, int videoDurationLimit, Context context) {
		Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//		if(videoDurationLimit > 0) {
//			captureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, videoDurationLimit); // set the image file name
//		}
		VideoRecorderActivity.IntentBuilder b = new VideoRecorderActivity.IntentBuilder(context);
		b.to(outputFileUri);
		b.durationLimit(videoDurationLimit * 1000);
		Intent result = b.build();

		return result;
	}


	private static Intent getLabeledIntentForVideoCamera(Context context, Intent intent) {
		List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
		String packageName = "";
		if(resInfo != null && resInfo.isEmpty()) {
			packageName = resInfo.get(0).activityInfo.packageName;
		}
		return new LabeledIntent(intent, packageName, R.string.camcorder, R.drawable.camcorder_icon);
	}


	/**
	 * creates an app chooser for picking recent pictures from gallery
	 *
	 * @param galleryChooserTitle title of the chooser
	 * @return Intent that when run will initiate a gallery chooser for apps that can display recently used images
	 */
	private static Intent makeGalleryRecentChooserIntent(String galleryChooserTitle) {
		Intent galleryIntent = makeGalleryRecentIntent();
		return Intent.createChooser(galleryIntent, galleryChooserTitle);
	}


	/**
	 * creates an Intent that when run will start an app that lets the user pick from recently used images
	 *
	 * @return Intent that when run will start an app that can display recently used images
	 */
	private static Intent makeGalleryRecentIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		// The MIME data type filter
		intent.setType("image/*");
		// Only return URIs that can be opened with ContentResolver
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		// Create the chooser Intent
		return intent;
	}


	/**
	 * creates a camera chooser for camera with all the apps on the device that support taking pictures or videos
	 *
	 * @param context            context of the calling app to resolve the applications that are installed on the phone
	 * @param outputPhotoUri     Uri where the captured image should be saved
	 * @param outputVideoUri     Uri where the captured video should be saved
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Intent that when run will initiate an app chooser
	 */
	private static Intent makePhotoVideoCameraChooserIntent(Context context, Uri outputPhotoUri, Uri outputVideoUri, String cameraChooserTitle, int videoDurationLimit) {
		// Array for all desired intents - photo cameras, video cameras, galleries
		final List<Intent> cameraIntents = new ArrayList<>();
		final PackageManager packageManager = context.getPackageManager();

		// Photos
		final Intent capturePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final List<ResolveInfo> listPhotoCam = packageManager.queryIntentActivities(capturePhotoIntent, PackageManager.MATCH_DEFAULT_ONLY);
		for(ResolveInfo res : listPhotoCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(capturePhotoIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputPhotoUri);
			cameraIntents.add(intent);
		}

		// Videos
		final Intent captureVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		final List<ResolveInfo> listVideoCam = packageManager.queryIntentActivities(captureVideoIntent, PackageManager.MATCH_DEFAULT_ONLY);
		for(ResolveInfo res : listVideoCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureVideoIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputVideoUri);
			if(videoDurationLimit > 0) {
				intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, videoDurationLimit);
			}
			cameraIntents.add(intent);
		}

		// Create chooser with photo cameras
		final Intent chooserIntent = Intent.createChooser(makeGalleryIntent(), cameraChooserTitle);

		// Add other camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

		return chooserIntent;
	}


	/**
	 * creates a camera chooser for custom camera (cwac-cam2) that support taking pictures or videos
	 *
	 * @param context            context of the calling app to resolve the applications that are installed on the phone
	 * @param outputPhotoUri     Uri where the captured image should be saved
	 * @param outputVideoUri     Uri where the captured video should be saved
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Intent that when run will initiate an app chooser
	 */
	private static Intent makeCustomPhotoVideoCameraChooserIntent(Context context, Uri outputPhotoUri, Uri outputVideoUri, String cameraChooserTitle, int videoDurationLimit) {
		Intent photoIntent = getLabeledIntentForPhotoCamera(context, makeCustomCameraIntent(outputPhotoUri, context));
		Intent videoIntent = getLabeledIntentForVideoCamera(context, makeCustomVideoCameraIntent(outputVideoUri, videoDurationLimit, context));

		// Create chooser with photo cameras
		final Intent chooserIntent = Intent.createChooser(makeGalleryIntent(), cameraChooserTitle);

		// Add other camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{photoIntent, videoIntent});

		return chooserIntent;
	}


	/**
	 * creates an app chooser for picking pictures from gallery
	 *
	 * @param galleryChooserTitle title of the chooser
	 * @return Intent that when run will initiate a gallery chooser for apps that can display recently used images
	 */
	private static Intent makeGalleryChooserIntent(String galleryChooserTitle) {
		return Intent.createChooser(makeGalleryIntent(), galleryChooserTitle);
	}


	/**
	 * creates an Intent that when run will start an app that lets the user pick an image from gallery
	 *
	 * @return Intent that when run will start an app that can display recently used images
	 */
	private static Intent makeGalleryIntent() {
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		return intent;
	}


	/**
	 * creates an app chooser for both camera and gallery apps
	 *
	 * @param outputFileUri       Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @param galleryChooserTitle title of the app chooser
	 * @param context context of the app/activity
	 * @return Intent that when run will initiate an app chooser for all camera and gallery apps installed on the device
	 */
	private static Intent makeCameraGalleryChooserIntent(Uri outputFileUri, String galleryChooserTitle, Context context) {
		Intent cameraIntent = makeCameraIntent(outputFileUri);
		Intent galleryIntent = makeGalleryIntent();
		// Only return URIs that can be opened with ContentResolver
		Intent chooserIntent = Intent.createChooser(galleryIntent, galleryChooserTitle);
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
		return chooserIntent;
	}


	/**
	 * creates an app chooser for custom camera and gallery apps
	 *
	 * @param outputFileUri       Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @param galleryChooserTitle title of the app chooser
	 * @param context context of the app/activity
	 * @return Intent that when run will initiate an app chooser for all camera and gallery apps installed on the device
	 */
	private static Intent makeCustomCameraGalleryChooserIntent(Uri outputFileUri, String galleryChooserTitle, Context context) {
		Intent cameraIntent = getLabeledIntentForPhotoCamera(context, makeCustomCameraIntent(outputFileUri, context));
		Intent galleryIntent = makeGalleryIntent();
		// Only return URIs that can be opened with ContentResolver
		Intent chooserIntent = Intent.createChooser(galleryIntent, galleryChooserTitle);
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
		return chooserIntent;
	}


	/**
	 * Creates file where an image will be later stored and returns an Uri for this image,
	 * the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context context of the app/activity
	 * @param dirName name of the directory where the file should be stored
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static Uri createImageFileUri(Context context, String dirName) throws IOException {
		return Uri.fromFile(createImageFile(context, dirName));
	}


	/**
	 * Creates file where an video will be later stored and returns an Uri for this video,
	 * the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context context of the app/activity
	 * @param dirName name of the directory where the file should be stored
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static Uri createVideoFileUri(Context context, String dirName) throws IOException {
		return Uri.fromFile(createVideoFile(context, dirName));
	}


	/**
	 * Creates file where an image will be later stored,
	 * the file has a unique timestamp and is stored in the directory named based on the app package
	 * the file is created in external storage of the phone if it is available, in the internal otherwise
	 *
	 * @param context context of the app/activity
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createImageFile(Context context) throws IOException {
		return createImageFile(context, false);
	}


	/**
	 * Creates file where an image will be later stored,
	 * the file has a unique timestamp and is stored in the directory named based on the app package
	 *
	 * @param context            context of the app/activity
	 * @param createImageInCache flag if the file should be created in cache (if set to true) or in external storage (if set to false) - in external storage it will be stored permanently, in cache not
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createImageFile(Context context, boolean createImageInCache) throws IOException {
		return createImageFile(context, context.getString(context.getApplicationInfo().labelRes), createImageInCache);
	}


	/**
	 * Creates file where an image will be later stored, the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context            context of the app/activity
	 * @param dirName            name of the directory where the file should be stored
	 * @param createImageInCache flag if the file should be created in cache (if set to true) or in external storage (if set to false) - in external storage it will be stored permanently, in cache not
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createImageFile(Context context, String dirName, boolean createImageInCache) throws IOException {
		return createMediaFile(context, dirName, createImageInCache, false);
	}


	/**
	 * Creates file where an image will be later stored, the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context            context of the app/activity
	 * @param dirName            name of the directory where the file should be stored
	 * @param createImageInCache flag if the file should be created in cache (if set to true) or in external storage (if set to false) - in external storage it will be stored permanently, in cache not
	 * @param isVideo            flag if the file should be created for image or video
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createMediaFile(Context context, String dirName, boolean createImageInCache, boolean isVideo) throws IOException {
		// Create an image or video file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String mediaFileName;
		if(isVideo) {
			mediaFileName = "MPG4_" + timeStamp + "_";
		} else {
			mediaFileName = "JPEG_" + timeStamp + "_";
		}

		File mediaDir;
		String externalStorageState = Environment.getExternalStorageState();
		if(createImageInCache) {
			if(Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
				mediaDir = context.getExternalCacheDir();
			} else {
				mediaDir = context.getCacheDir();
			}
		} else {
			if(Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
				if(!isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					File[] mediaDirArray = context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES);
					mediaDir = getFirstNonNullItemInArray(mediaDirArray);
				} else {
					mediaDir = Environment.getExternalStoragePublicDirectory(isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES);
				}
			} else {
				mediaDir = context.getFilesDir();
			}
		}

		if(mediaDir == null) {
			throw new IOException("mediaDir == null");
		}

		final File storageDir = new File(mediaDir, dirName);
		final boolean mkDirsOk = storageDir.mkdirs();
		final boolean isDir = storageDir.isDirectory();
		if(!(mkDirsOk || isDir)) {
			throw new IOException("!(mkDirsOk || isDir)");
		}

		if(isVideo) {
			return File.createTempFile(
					mediaFileName,  /* prefix */
					".mp4",         /* suffix */
					storageDir      /* directory */
			);
		} else {
			return File.createTempFile(
					mediaFileName,  /* prefix */
					".jpg",         /* suffix */
					storageDir      /* directory */
			);
		}

	}


	/**
	 * Creates file where an image will be later stored, the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context context of the app/activity
	 * @param dirName name of the directory where the file should be store, the file will be created in external storage
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createImageFile(Context context, String dirName) throws IOException {
		return createImageFile(context, dirName, false);
	}


	/**
	 * Creates file where an video will be later stored, the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context            context of the app/activity
	 * @param dirName            name of the directory where the file should be stored
	 * @param createImageInCache flag if the file should be created in cache (if set to true) or in external storage (if set to false) - in external storage it will be stored permanently, in cache not
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createVideoFile(Context context, String dirName, boolean createImageInCache) throws IOException {
		return createMediaFile(context, dirName, createImageInCache, true);
	}


	/**
	 * Creates file where an video will be later stored, the file has a unique timestamp and is stored in the directory provided in the parameter
	 *
	 * @param context context of the app/activity
	 * @param dirName name of the directory where the file should be store, the file will be created in external storage
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createVideoFile(Context context, String dirName) throws IOException {
		return createVideoFile(context, dirName, false);
	}


	/**
	 * Creates file where an video will be later stored,
	 * the file has a unique timestamp and is stored in the directory named based on the app package
	 * the file is created in external storage of the phone if it is available, in the internal otherwise
	 *
	 * @param context context of the app/activity
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createVideoFile(Context context) throws IOException {
		return createVideoFile(context, false);
	}


	/**
	 * Creates file where an video will be later stored,
	 * the file has a unique timestamp and is stored in the directory named based on the app package
	 *
	 * @param context            context of the app/activity
	 * @param createImageInCache flag if the file should be created in cache (if set to true) or in external storage (if set to false) - in external storage it will be stored permanently, in cache not
	 * @return File that was created if it succeeded, null otherwise
	 * @throws IOException is thrown if the file creation was not successful because of some I/O failure
	 */
	private static File createVideoFile(Context context, boolean createImageInCache) throws IOException {
		return createVideoFile(context, context.getString(context.getApplicationInfo().labelRes), createImageInCache);
	}


	private static long getVideoLength(File videoFile) {
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(videoFile.getAbsolutePath());
		String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		long timeInMillis = Long.parseLong(time);
		return TimeUnit.MILLISECONDS.toSeconds(timeInMillis);
	}


	/**
	 * publishes photo to phone's photo gallery without a need of permission
	 *
	 * @param context      context of the app/activity
	 * @param mediaFileUri uri of the file that should be made public in the phone's gallery
	 */
	private static void publishMediaToSystemGallery(Context context, Uri mediaFileUri) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(mediaFileUri);
		context.sendBroadcast(mediaScanIntent);
	}


	/**
	 * gets the app package name from application info
	 *
	 * @param context context of the application/activity
	 * @return a String containing a package name of the app using this PhotoManager
	 */
	private static String getDefaultDir(Context context) {
		return context.getString(context.getApplicationInfo().labelRes);
	}


	/**
	 * extracts the first non null element in an array, returns null if the array is null or if there is no non-null item
	 *
	 * @param array source array from which the non-null item should be extracted
	 * @return the first non-null item in the given array, returns null if all of the items in the array are null or if an array is null
	 */
	@Nullable
	private static File getFirstNonNullItemInArray(File[] array) {
		if(array != null) {
			for(File mediaDirItem : array) {
				if(mediaDirItem != null) {
					return mediaDirItem;
				}
			}
		}
		return null;
	}


	/**
	 * launches a camera app that is installed on the phone, launches an app picker if more options are available
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param galleryDirName     name of directory where the taken picture is supposed to be stored
	 * @return Uri of the image that where the captured image will be stored
	 * @throws IOException is thrown if the image file creation was not successful
	 */
	public static Uri launchCameraOnly(Fragment fragment, String cameraChooserTitle, String galleryDirName) throws IOException {
		Uri uri = createImageFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		Intent cameraLauncher = makeCameraChooserIntent(uri, cameraChooserTitle);
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);

		return uri;
	}


	/**
	 * launches a camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @return Uri of the image that where the captured image will be stored
	 * @throws IOException is thrown if the image file creation was not successful
	 */
	public static Uri launchCameraOnly(Fragment fragment, String cameraChooserTitle) throws IOException {
		return launchCameraOnly(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()));
	}


	/**
	 * launches a camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment                fragment calling the camera intent
	 * @param cameraChooserTitleResId string resolution id of a title of the camera app chooser
	 * @return Uri of the image that where the captured image will be stored
	 * @throws IOException is thrown if the image file creation was not successful
	 */
	public static Uri launchCameraOnly(Fragment fragment, @StringRes int cameraChooserTitleResId) throws IOException {
		return launchCameraOnly(fragment, fragment.getString(cameraChooserTitleResId));
	}


	/**
	 * launches a custom camera(cwac-cam2)
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param galleryDirName     name of directory where the taken picture is supposed to be stored
	 * @return Uri of the image that where the captured image will be stored
	 * @throws IOException is thrown if the image file creation was not successful
	 */
	public static Uri launchCustomCameraOnly(Fragment fragment, String galleryDirName) throws IOException {
		Uri uri = createImageFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		Intent cameraLauncher = makeCustomCameraIntent(uri, fragment.getContext());
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);

		return uri;
	}


	/**
	 * launches a video camera app that is installed on the phone, launches an app picker if more options are available
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param galleryDirName     name of directory where the taken video is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Uri launchVideoCameraOnly(Fragment fragment, String cameraChooserTitle, int videoDurationLimit, String galleryDirName) throws IOException {
		Uri uri = createVideoFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		Intent cameraLauncher = makeVideoCameraChooserIntent(uri, cameraChooserTitle, videoDurationLimit);
		fragment.startActivityForResult(cameraLauncher, REQUEST_VIDEO_CAPTURE);

		return uri;
	}


	/**
	 * launches a video camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Uri launchVideoCameraOnly(Fragment fragment, String cameraChooserTitle, int videoDurationLimit) throws IOException {
		return launchVideoCameraOnly(fragment, cameraChooserTitle, videoDurationLimit, getDefaultDir(fragment.getContext()));
	}


	/**
	 * launches a video camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment                fragment calling the camera intent
	 * @param cameraChooserTitleResId string resolution id of a title of the camera app chooser
	 * @param videoDurationLimit      limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Uri launchVideoCameraOnly(Fragment fragment, @StringRes int cameraChooserTitleResId, int videoDurationLimit) throws IOException {
		return launchVideoCameraOnly(fragment, fragment.getString(cameraChooserTitleResId), videoDurationLimit);
	}


	/**
	 * launches custom video camera
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param galleryDirName     name of directory where the taken video is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Uri launchCustomVideoCameraOnly(Fragment fragment, int videoDurationLimit, String galleryDirName) throws IOException {
		Uri uri = createVideoFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		Intent cameraLauncher = makeCustomVideoCameraIntent(uri, videoDurationLimit, fragment.getContext());
		fragment.startActivityForResult(cameraLauncher, REQUEST_VIDEO_CAPTURE);

		return uri;
	}


	/**
	 * launches a video camera app or photo camera app that is installed on the phone, launches an app picker
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param galleryDirName     name of directory where the taken video is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle, String galleryDirName, int videoDurationLimit) throws IOException {
		Uri videoUri = createVideoFileUri(fragment.getContext(), galleryDirName);
		Uri photoUri = createImageFileUri(fragment.getContext(), galleryDirName);
		if(videoUri == null || photoUri == null) {
			throw new IOException("PhotoManager: Cannot create file uri for photo or video");
		}

		Intent cameraLauncher = makeCustomPhotoVideoCameraChooserIntent(fragment.getContext(), photoUri, videoUri, cameraChooserTitle, videoDurationLimit);
		fragment.startActivityForResult(cameraLauncher, REQUEST_PHOTO_VIDEO_CAPTURE);

		return new Pair<>(photoUri, videoUri);
	}


	/**
	 * launches custom photo and video camera app or photo camera app that is installed on the phone, launches an app picker
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param galleryDirName     name of directory where the taken video is supposed to be stored
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchCustomPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle, String galleryDirName, int videoDurationLimit) throws IOException {
		Uri videoUri = createVideoFileUri(fragment.getContext(), galleryDirName);
		Uri photoUri = createImageFileUri(fragment.getContext(), galleryDirName);
		if(videoUri == null || photoUri == null) {
			throw new IOException("PhotoManager: Cannot create file uri for photo or video");
		}

		Intent cameraLauncher = makeCustomPhotoVideoCameraChooserIntent(fragment.getContext(), photoUri, videoUri, cameraChooserTitle, videoDurationLimit);
		fragment.startActivityForResult(cameraLauncher, REQUEST_PHOTO_VIDEO_CAPTURE);

		return new Pair<>(photoUri, videoUri);
	}


	/**
	 * launches a video camera app or photo camera app that is installed on the phone, launches an app picker
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param galleryDirName     name of directory where the taken video is supposed to be stored
	 * @return Uri of the video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle, String galleryDirName) throws IOException {
		return launchPhotoVideoCameraGallery(fragment, cameraChooserTitle, galleryDirName, 0);
	}


	/**
	 * launches a video camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Pair<Uri,Uri> of the photo and video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle, int videoDurationLimit) throws IOException {
		return launchPhotoVideoCameraGallery(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()), videoDurationLimit);
	}


	/**
	 * launches a custom photo and video camera app, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @param videoDurationLimit limit the video duration in seconds, if set to 0, there's no limitation
	 * @return Pair<Uri,Uri> of the photo and video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchCustomPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle, int videoDurationLimit) throws IOException {
		return launchCustomPhotoVideoCameraGallery(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()), videoDurationLimit);
	}


	/**
	 * launches a video camera app that is installed on the phone, launches an app picker if more options are available
	 * the directory name where the image is stored is based on the package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment calling the camera intent
	 * @param cameraChooserTitle title of the camera app chooser
	 * @return Pair<Uri,Uri> of the photo and video that where the captured video will be stored
	 * @throws IOException is thrown if the video file creation was not successful
	 */
	public static Pair<Uri, Uri> launchPhotoVideoCameraGallery(Fragment fragment, String cameraChooserTitle) throws IOException {
		return launchPhotoVideoCameraGallery(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()));
	}


	/**
	 * launches an image picker of recently used images
	 *
	 * @param fragment            fragment calling the recent pictures intent
	 * @param galleryChooserTitle title of the gallery app chooser
	 */
	public static void launchGalleryRecentOnly(Fragment fragment, String galleryChooserTitle) {
		Intent cameraLauncher = makeGalleryRecentChooserIntent(galleryChooserTitle);
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);
	}


	/**
	 * launches an image picker of recently used images
	 *
	 * @param fragment                 fragment calling the recent pictures intent
	 * @param galleryChooserTitleResId string resolution id of a title of the gallery app chooser
	 */
	public static void launchGalleryRecentOnly(Fragment fragment, @StringRes int galleryChooserTitleResId) {
		launchGalleryRecentOnly(fragment, fragment.getString(galleryChooserTitleResId));
	}


	/**
	 * launches gallery from which the user can pick an image
	 *
	 * @param fragment            fragment that started the gallery picker
	 * @param galleryChooserTitle title of the app chooser for the case when there are more gallery apps installed on the device
	 */
	public static void launchGalleryOnly(Fragment fragment, String galleryChooserTitle) {
		Intent cameraLauncher = makeGalleryChooserIntent(galleryChooserTitle);
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);
	}


	/**
	 * launches gallery from which the user can pick an image
	 *
	 * @param fragment                 fragment that started the gallery picker
	 * @param galleryChooserTitleResId string resolution id of the title of the app chooser for the case when there are more gallery apps installed on the device
	 */
	public static void launchGalleryOnly(Fragment fragment, @StringRes int galleryChooserTitleResId) {
		launchGalleryOnly(fragment, fragment.getString(galleryChooserTitleResId));
	}


	/**
	 * launches an app picker with all camera and gallery apps installed on the device
	 *
	 * @param fragment           fragment starting the camera/gallery app
	 * @param cameraChooserTitle title of the app chooser
	 * @param galleryDirName     directory name of the gallery where the picture from camera is supposed to be stored
	 * @param context context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCameraGallery(Fragment fragment, String cameraChooserTitle, String galleryDirName, Context context) throws IOException {
		Uri uri = createImageFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		final Intent cameraLauncher = makeCameraGalleryChooserIntent(uri, cameraChooserTitle, context);
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);

		return uri;
	}


	/**
	 * launches an app picker with custom camera(cwac-cam2) and gallery apps installed on the device
	 *
	 * @param fragment           fragment starting the camera/gallery app
	 * @param cameraChooserTitle title of the app chooser
	 * @param galleryDirName     directory name of the gallery where the picture from camera is supposed to be stored
	 * @param context context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCustomCameraGallery(Fragment fragment, String cameraChooserTitle, String galleryDirName, Context context) throws IOException {
		Uri uri = createImageFileUri(fragment.getContext(), galleryDirName);

		if(uri == null) {
			throw new IOException();
		}

		final Intent cameraLauncher = makeCustomCameraGalleryChooserIntent(uri, cameraChooserTitle, context);
		fragment.startActivityForResult(cameraLauncher, REQUEST_IMAGE_CAPTURE);

		return uri;
	}


	/**
	 * launches an app picker with all camera and gallery apps installed on the device
	 * directory name of the gallery where the picture from camera is supposed to be stored is extracted from the app package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment starting the camera/gallery app
	 * @param cameraChooserTitle title of the app chooser
	 * @param context context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCameraGallery(Fragment fragment, String cameraChooserTitle, Context context) throws IOException {
		return launchCameraGallery(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()), context);
	}


	/**
	 * launches an app picker with custom camera(cwac-cam2) and gallery apps installed on the device
	 * directory name of the gallery where the picture from camera is supposed to be stored is extracted from the app package name of the app using this PhotoManager
	 *
	 * @param fragment           fragment starting the camera/gallery app
	 * @param cameraChooserTitle title of the app chooser
	 * @param context            context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCustomCameraGallery(Fragment fragment, String cameraChooserTitle, Context context) throws IOException {
		return launchCustomCameraGallery(fragment, cameraChooserTitle, getDefaultDir(fragment.getContext()), context);
	}


	/**
	 * launches an app picker with all camera and gallery apps installed on the device
	 * directory name of the gallery where the picture from camera is supposed to be stored is extracted from the app package name of the app using this PhotoManager
	 *
	 * @param fragment                fragment starting the camera/gallery app
	 * @param cameraChooserTitleResId string resolution id of the title of the app chooser
	 * @param context context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCameraGallery(Fragment fragment, @StringRes int cameraChooserTitleResId, Context context) throws IOException {
		return launchCameraGallery(fragment, fragment.getString(cameraChooserTitleResId), context);
	}


	/**
	 * launches an app picker with custom camera(cwac-cam2) and gallery apps installed on the device
	 * directory name of the gallery where the picture from camera is supposed to be stored is extracted from the app package name of the app using this PhotoManager
	 *
	 * @param fragment                fragment starting the camera/gallery app
	 * @param cameraChooserTitleResId string resolution id of the title of the app chooser
	 * @param context context of the app/activity
	 * @return Uri of the file where the captured image from camera will be stored (image from gallery will return its own Uri)
	 * @throws IOException is thrown if the file creation for captured image has failed
	 */
	public static Uri launchCustomCameraGallery(Fragment fragment, @StringRes int cameraChooserTitleResId, Context context) throws IOException {
		return launchCustomCameraGallery(fragment, fragment.getString(cameraChooserTitleResId), context);
	}


	/**
	 * extracts File from a Uri that is of 'file' scheme
	 *
	 * @param uri Uri with a 'file' scheme - starting 'file://'
	 * @return File extracted from Uri if it succeeds (the permission to read the Uri is granted), null otherwise
	 */
	public static File getFileFromUri(Uri uri) {
		return new File(uri.getPath());
	}


	/**
	 * loads File from Uri and returns the result in a listener callback
	 *
	 * @param context  context of the app or activity
	 * @param uri      Uri from which the File should be extracted
	 * @param isPhoto  indicator of type of file
	 * @param listener listener to provide the resulting File
	 * @throws SecurityException is thrown if the user doesn't have a permission to read the Uri (some gallery apps don't give your app correct permission to read the file
	 *                           on the Uri - this needs to be handled on Marshmallow and newer devices to prompt the user to grant the permission
	 */
	public static Uri loadFileFromUri(final Context context, Uri uri, boolean isPhoto, OnFileFromUriExtractedListener listener) throws SecurityException {
		Uri fileUri = uri;
		//in case of the image was saved from camera
		if(uri.getScheme().equals("file")) {
			File file = getFileFromUri(uri);
			//if the file cannot be read, there might be a problem with read permission not being granted, this can happen e.g. when the file is picked from Dropbox
			if(!file.canRead() && file.exists()) {
				throw new SecurityException();
			}
			if(listener != null) {
				listener.onFileFromUriExtracted(file, false);
			}

		} //in case the image was saved from gallery
		else if(uri.getScheme().equals("content")) {
			InputStream i = null;
			try {
				i = context.getContentResolver().openInputStream(uri);
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			}
			if(i != null) {
				BufferedInputStream bufferedInputStream = new BufferedInputStream(i);
				new LoadFileFromInputStreamAsyncTask(context, listener).execute(bufferedInputStream);
			}
		}
		return fileUri;
	}


	/**
	 * helper method that wraps up everything that needs to be done in fragment's onActivityResult after taking a picture/picking a picture from a gallery
	 *
	 * @param fragment    fragment that calls this method
	 * @param requestCode request code with which the onActivityResult method in fragment was called
	 * @param resultCode  result code with which the onActivityResult method in fragment was called
	 * @param data        data of onActivityResult in fragment
	 * @param mediaUri    photo Uri - needs to be stored in the fragment and provided from the fragment if the camera was chosen because this photo Uri is then not provided in the Intent of onActivityResult,
	 *                    only gallery returns Uri in Intent of onActivityResult
	 * @param listener    callback listener that provides the file (can be null, if the user doesn't require to
	 * @return photoUri if the file can be created from uri, null if something failed or if the permission to read uri was not granted (in this case a request for permission was automatically initiated)
	 */
	@CheckResult
	public static Uri onActivityResult(final Fragment fragment, int requestCode, int resultCode, Intent data, Uri mediaUri, OnFileFromUriExtractedListener listener) {
		//process request normally if the result was OK, if not, delete the temp file if it was created for a photo
		if(resultCode == Activity.RESULT_OK) {
			if(requestCode == PhotoManager.REQUEST_IMAGE_CAPTURE) {

				//this happens if the picture is chosen from the gallery
				if(data != null && data.getData() != null) {
					//delete file on this Uri because a picture was chosen from gallery and therefore the temp file where the captured photo was supposed to be saved wasn't used
					if(mediaUri != null && !mediaUri.equals(data.getData())) {
						deleteFileForUri(mediaUri);
					}
					mediaUri = data.getData();
				}

				if(mediaUri == null) return null;

				try {
					loadFileFromUri(fragment.getContext(), mediaUri, true, listener);
					PhotoManager.publishMediaToSystemGallery(fragment.getContext(), mediaUri);
				} catch(SecurityException e) {
					e.printStackTrace();
					checkReadExternalStoragePermission(fragment, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
					return null;
				}
				return mediaUri;
			} else if(requestCode == PhotoManager.REQUEST_VIDEO_CAPTURE) {
				if(mediaUri == null) return null;

				mediaUri = loadFileFromUri(fragment.getContext(), mediaUri, false, listener);
				return mediaUri;
			} else {
				return null;
			}
		} else {
			deleteFileForUri(mediaUri);
			return null;
		}
	}


	/**
	 * helper method that wraps up everything that needs to be done in fragment's onActivityResult after taking a picture/picking a picture from a gallery
	 *
	 * @param fragment    fragment that calls this method
	 * @param requestCode request code with which the onActivityResult method in fragment was called
	 * @param resultCode  result code with which the onActivityResult method in fragment was called
	 * @param data        data of onActivityResult in fragment
	 * @param photoUri    photo Uri - needs to be stored in the fragment and provided from the fragment if the camera was chosen because this photo Uri is then not provided in the Intent of onActivityResult,
	 *                    only gallery returns Uri in Intent of onActivityResult
	 * @return photoUri if the file can be created from uri, null if something failed or if the permission to read uri was not granted (in this case a request for permission was automatically initiated)
	 */
	@CheckResult
	public static Uri onActivityResult(Fragment fragment, int requestCode, int resultCode, Intent data, Uri photoUri) {
		return onActivityResult(fragment, requestCode, resultCode, data, photoUri, null);
	}


	/**
	 * deletes file at a given uri if the uri is not null and if the file exists
	 *
	 * @param uri uri at which the file is saved
	 * @return true if the file was successfully deleted, false otherwise
	 */
	public static boolean deleteFileForUri(Uri uri) {
		if(uri != null) {
			File file = getFileFromUri(uri);
			if(file.exists()) {
				return file.delete();
			}
		}
		return false;
	}


	/**
	 * scales and if necessary adjusts rotation an image to required width and height and returns result in the listener callback
	 *
	 * @param context   context of tha app/activity
	 * @param imageFile image file that should be scaled
	 * @param reqWidth  required width of the output image
	 * @param reqHeight required height of the output image
	 * @param listener  listener that will be used to provide the calling fragment the resulting scaled image
	 */
	public static void scaleImageFile(final Context context, final File imageFile, int reqWidth, int reqHeight, ScaleImageAsyncTask.OnFileScaledListener listener) {
		new ScaleImageAsyncTask(context, reqWidth, reqHeight, listener).execute(imageFile);
	}


	/**
	 * Checks permission and if it is not granted, shows a dialog to deny/grant permission, if it was denied with 'don't show again' it shows a snackbar with a button to access settings
	 *
	 * @param fragment    fragment that is calling the request to check permission (this fragment should override onRequestPermissionResult()
	 * @param requestCode request code that is then used in onRequestPermissionsResult method to identify the permission
	 * @return true if the permission is already granted, false if it is not
	 */
	public static boolean checkReadExternalStoragePermission(Fragment fragment, int requestCode) {
		if(Build.VERSION.SDK_INT > 15 && ContextCompat.checkSelfPermission(fragment.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			fragment.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
			return false;
		} else { //the permission is granted
			return true;
		}
	}


	//asynchronous loading of a File from InputStream
	private static class LoadFileFromInputStreamAsyncTask extends AsyncTask<BufferedInputStream, Void, File> {

		private OnFileFromUriExtractedListener mListener;
		private Context mContext;


		public LoadFileFromInputStreamAsyncTask(Context context, OnFileFromUriExtractedListener listener) {
			mListener = listener;
			mContext = context;
		}


		@Override
		protected File doInBackground(BufferedInputStream... params) {
			return getFileFromInputStream(mContext, params[0]);
		}


		@Override
		protected void onPostExecute(File file) {
			mListener.onFileFromUriExtracted(file, false);
		}


		/**
		 * creates a file from an inputStream
		 *
		 * @param context     context of the app/activity necessary to create a file
		 * @param inputStream input stream from which the file is supposed to be created
		 * @return file from the input stream if the process was successful, null otherwise
		 */
		private File getFileFromInputStream(Context context, BufferedInputStream inputStream) {

			try {
				File file = createImageFile(context, true);

				OutputStream output = new FileOutputStream(file);
				byte[] buffer = new byte[4 * 1024];
				int read;

				while((read = inputStream.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
				output.flush();
				return file;

			} catch(IOException e) {
				e.printStackTrace();
			} finally {
				if(inputStream != null) {
					try {
						inputStream.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}

			return null;
		}
	}
}