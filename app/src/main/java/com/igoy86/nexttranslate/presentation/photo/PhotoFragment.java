package com.igoy86.nexttranslate.presentation.photo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.igoy86.nexttranslate.MainActivity;
import com.igoy86.nexttranslate.databinding.FragmentPhotoBinding;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.presentation.translate.TranslateFragment;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.igoy86.nexttranslate.presentation.translate.LanguagePickerBottomSheet;

/**
 * PhotoFragment — CameraX live viewfinder with ML Kit OCR translation.
 *
 * <p>Provides a full-screen camera preview powered by CameraX. When the user
 * taps the shutter button, a JPEG is captured in-memory, passed to ML Kit
 * Text Recognition for OCR, and the extracted text is displayed in a
 * bottom result panel. Tapping "Terjemahkan" switches to
 * {@link TranslateFragment} with the OCR text pre-filled.</p>
 *
 * <p>Permission flow:</p>
 * <ul>
 *     <li>On first open, {@link #cameraPermissionLauncher} requests
 *         {@link Manifest.permission#CAMERA}.</li>
 *     <li>If granted → camera binds immediately.</li>
 *     <li>If denied  → placeholder message shown.</li>
 * </ul>
 *
 * <p>CameraX use-cases bound:</p>
 * <ul>
 *     <li>{@link Preview}      — live viewfinder surface</li>
 *     <li>{@link ImageCapture} — single JPEG capture for OCR</li>
 * </ul>
 */
public class PhotoFragment extends BaseFragment<FragmentPhotoBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "PhotoFragment";
	
	/** Currently selected source language BCP-47 code. */
    private String sourceLangCode = "en";

    /** Currently selected target language BCP-47 code. */
    private String targetLangCode = "id";

    /**
     * CameraX ImageCapture use-case.
     * Initialized in {@link #startCamera()} and used by {@link #captureAndOcr()}.
     */
    private ImageCapture imageCapture;

    /**
     * Single-thread executor for CameraX operations.
     * Shut down in {@link #onDestroyView()}.
     */
    private ExecutorService cameraExecutor;

    /**
     * ML Kit on-device Latin text recognizer.
     * Reused across captures; closed in {@link #onDestroyView()}.
     */
    private TextRecognizer textRecognizer;
	
	/** CameraX Camera instance — used for flash and focus control. */
    private androidx.camera.core.Camera camera;

    /** Current flash state. */
    private boolean isFlashOn = false;

    /**
     * Launcher for the CAMERA runtime permission request.
     * Registered before Fragment is attached (in field initializer).
     */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            FileLogger.d(TAG, "Camera permission granted.");
                            startCamera();
                        } else {
                            FileLogger.d(TAG, "Camera permission denied.");
                            showToast("Camera permission is required for this feature. Please allow access to continue..");
                        }
                    });
					
	/**
	 * Launcher for the system image picker.
     * Opens the gallery and returns the selected image URI for OCR processing.
     */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        FileLogger.d(TAG, "Gallery image selected: " + uri);
                        processImageFromUri(uri);
                    });

    // -------------------------------------------------------------------------
    // BaseFragment contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Inflates {@code fragment_photo.xml} using ViewBinding.</p>
     */
    @NonNull
    @Override
    protected FragmentPhotoBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentPhotoBinding.inflate(inflater, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes the camera executor, ML Kit text recognizer, and
     * applies status-bar top inset. Requests camera permission if not yet
     * granted, otherwise starts the camera immediately.</p>
     */
    @Override
    protected void initViews() {
        cameraExecutor  = Executors.newSingleThreadExecutor();
        textRecognizer  = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Apply status bar inset so toolbar/language bar clears the status bar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().rootPhoto, (v, insets) -> {
                    final int top = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                    v.setPadding(v.getPaddingLeft(), top,
                            v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        if (hasCameraPermission()) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>No LiveData to observe — OCR result is handled directly
     * via ML Kit callbacks on the main thread.</p>
     */
    @Override
    protected void initObservers() {
        // No ViewModel in this fragment — OCR result handled via callbacks
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wires:</p>
     * <ul>
     *     <li>Shutter button     → {@link #captureAndOcr()}</li>
     *     <li>Translate button   → sends OCR text to {@link TranslateFragment}</li>
     *     <li>Swap lang button   → swaps source/target language labels</li>
     * </ul>
     */
    @Override
    protected void initListeners() {
        // Shutter — capture photo and run OCR
        getBinding().btnCapture.setOnClickListener(v -> captureAndOcr());

        // Translate — take OCR result to TranslateFragment
        getBinding().btnTranslate.setOnClickListener(v -> {
            final String ocrText = getBinding().tvOcrResult.getText().toString().trim();
            if (ocrText.isEmpty()) return;
            sendToTranslate(ocrText);
        });

        // Swap language labels
        getBinding().btnSwapLang.setOnClickListener(v -> swapLanguageLabels());
		
		// Gallery — pick image from gallery for OCR
        getBinding().btnGallery.setOnClickListener(v ->
                galleryLauncher.launch("image/*"));
				
		 // Flash toggle
        getBinding().btnFlash.setOnClickListener(v -> {
            if (camera == null) return;
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            getBinding().btnFlash.setImageResource(
                    isFlashOn
                            ? com.igoy86.nexttranslate.R.drawable.ic_flash_on
                            : com.igoy86.nexttranslate.R.drawable.ic_flash_off);
            FileLogger.d(TAG, "Flash toggled: " + isFlashOn);
        });

        // Tap-to-focus on preview
        getBinding().previewView.setOnTouchListener((v, event) -> {
            if (camera == null) return false;
            if (event.getAction() != android.view.MotionEvent.ACTION_UP) return true;

            final androidx.camera.core.MeteringPointFactory factory =
                    getBinding().previewView.getMeteringPointFactory();
            final androidx.camera.core.MeteringPoint point =
                    factory.createPoint(event.getX(), event.getY());
            final androidx.camera.core.FocusMeteringAction action =
                    new androidx.camera.core.FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build();
            camera.getCameraControl().startFocusAndMetering(action);
            FileLogger.d(TAG, "Tap-to-focus at: " + event.getX() + ", " + event.getY());
            v.performClick();
            return true;
        });
		
		// Source language tap
        getBinding().tvSourceLang.setOnClickListener(v -> {
            final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet
                    .newInstance(LanguagePickerBottomSheet.MODE_SOURCE, sourceLangCode);
            sheet.setOnLanguageSelectedListener((code, mode) -> {
                sourceLangCode = code;
                getBinding().tvSourceLang.setText(code.toUpperCase());
                FileLogger.d(TAG, "Source lang changed to: " + code);
            });
            sheet.show(getChildFragmentManager(), LanguagePickerBottomSheet.TAG);
        });

        // Target language tap
        getBinding().tvTargetLang.setOnClickListener(v -> {
            final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet
                    .newInstance(LanguagePickerBottomSheet.MODE_TARGET, targetLangCode);
            sheet.setOnLanguageSelectedListener((code, mode) -> {
                targetLangCode = code;
                getBinding().tvTargetLang.setText(code.toUpperCase());
                FileLogger.d(TAG, "Target lang changed to: " + code);
            });
            sheet.show(getChildFragmentManager(), LanguagePickerBottomSheet.TAG);
        });
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Shuts down the camera executor and closes the ML Kit recognizer
     * to release resources when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        super.onDestroyView();
    }

    // -------------------------------------------------------------------------
    // Camera
    // -------------------------------------------------------------------------

    /**
     * Binds CameraX {@link Preview} and {@link ImageCapture} use-cases
     * to this Fragment's lifecycle using the back-facing camera.
     *
     * <p>Must be called only after camera permission is granted.</p>
     */
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                final ProcessCameraProvider cameraProvider = future.get();

                // Preview use-case — live viewfinder
                final Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(getBinding().previewView.getSurfaceProvider());

                // ImageCapture use-case — for OCR snapshot
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Use back camera
                final CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind all before rebinding
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture);

                FileLogger.d(TAG, "CameraX bound successfully.");

            } catch (ExecutionException | InterruptedException e) {
                FileLogger.d(TAG, "CameraX bind failed: " + e.getMessage());
                showToast("Failed open the camera.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
	
	/**
     * Processes an image from the given URI using ML Kit OCR.
     * Called after the user picks an image from the gallery.
     *
     * @param uri the URI of the selected image
     */
    private void processImageFromUri(@NonNull android.net.Uri uri) {
        try {
            final android.graphics.Bitmap raw = android.provider.MediaStore.Images.Media
                    .getBitmap(requireContext().getContentResolver(), uri);
            final android.graphics.Bitmap processed = preprocessBitmap(raw);
            raw.recycle();
            final InputImage inputImage = InputImage.fromBitmap(processed, 0);
            textRecognizer.process(inputImage)
                        .addOnSuccessListener(visionText -> {
                            final String extracted = visionText.getText().trim();
                            FileLogger.d(TAG, "Gallery OCR result: " + extracted);
                            handleOcrSuccess(extracted);
                        })
                        .addOnFailureListener(e -> {
                            FileLogger.d(TAG, "Gallery OCR failed: " + e.getMessage());
                            showToast("Could not read text from image.");
                        });
            } catch (java.io.IOException e) {
                FileLogger.d(TAG, "Failed to load gallery image: " + e.getMessage());
                showToast("Failed to load image.");
            }
    }

    // -------------------------------------------------------------------------
    // OCR
    // -------------------------------------------------------------------------

    /**
     * Captures a single JPEG frame in-memory and passes it to ML Kit
     * Text Recognition for OCR processing.
     *
     * <p>On success, {@link #handleOcrSuccess(String)} is called with the
     * extracted text. On failure, a Toast is shown.</p>
     */
    private void captureAndOcr() {
        if (imageCapture == null) {
            showToast("Camera is not ready.");
            return;
        }

        // Capture in-memory (no disk write needed)
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageCapturedCallback() {

                    /**
                     * Called when the image capture succeeds.
                     * Converts the {@link ImageProxy} to an {@link InputImage}
                     * and runs ML Kit OCR.
                     *
                     * @param imageProxy the captured image proxy; must be closed after use
                     */
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        FileLogger.d(TAG, "Image captured, running OCR...");

                        // Convert ImageProxy to Bitmap for preprocessing
                        final android.graphics.Bitmap rawBitmap = imageProxy.toBitmap();
                        final android.graphics.Bitmap processed = preprocessBitmap(rawBitmap);
                        rawBitmap.recycle();

                        final InputImage inputImage = InputImage.fromBitmap(
                                processed,
                                imageProxy.getImageInfo().getRotationDegrees());

                        textRecognizer.process(inputImage)
                                .addOnSuccessListener(visionText -> {
                                    imageProxy.close();
									turnOffFlash();
                                    final String extracted = visionText.getText().trim();
                                    FileLogger.d(TAG, "OCR result: " + extracted);
                                    handleOcrSuccess(extracted);
                                })
                                .addOnFailureListener(e -> {
                                    imageProxy.close();
									turnOffFlash();
                                    FileLogger.d(TAG, "OCR failed: " + e.getMessage());
                                    showToast("Unable to read text");
                                });
                    }

                    /**
                     * Called when the image capture fails.
                     *
                     * @param exception the capture error
                     */
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
						turnOffFlash();
                        FileLogger.d(TAG, "Capture error: " + exception.getMessage());
                        showToast("Failed to capture photo.");
                    }
                });
    }
	
	/**
     * Turns off the torch and updates the flash button icon.
     * Called automatically after every capture attempt.
     */
    private void turnOffFlash() {
        if (camera == null || !isFlashOn) return;
        isFlashOn = false;
        camera.getCameraControl().enableTorch(false);
        getBinding().btnFlash.setImageResource(
                com.igoy86.nexttranslate.R.drawable.ic_flash_off);
        FileLogger.d(TAG, "Flash auto-off after capture.");
    }
	
	/**
     * Preprocesses a Bitmap before OCR to improve text recognition accuracy.
     * Applies grayscale conversion and contrast enhancement.
     *
     * @param original the original captured bitmap
     * @return preprocessed grayscale high-contrast bitmap
     */
    @NonNull
    private android.graphics.Bitmap preprocessBitmap(@NonNull android.graphics.Bitmap original) {
        // Step 1: Convert to grayscale
        android.graphics.Bitmap grayscale = android.graphics.Bitmap.createBitmap(
                original.getWidth(), original.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(grayscale);
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0); // remove color — pure grayscale
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(original, 0, 0, paint);

        // Step 2: Boost contrast via ColorMatrix
        // Scale RGB channels (1.5x) and shift brightness (-60) for sharper text
        android.graphics.Bitmap contrasted = android.graphics.Bitmap.createBitmap(
                grayscale.getWidth(), grayscale.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas2 = new android.graphics.Canvas(contrasted);
        android.graphics.ColorMatrix contrastMatrix = new android.graphics.ColorMatrix(new float[]{
                1.5f,  0,    0,    0, -60,
                0,     1.5f, 0,    0, -60,
                0,     0,    1.5f, 0, -60,
                0,     0,    0,    1,   0
        });
        android.graphics.Paint paint2 = new android.graphics.Paint();
        paint2.setColorFilter(new android.graphics.ColorMatrixColorFilter(contrastMatrix));
        canvas2.drawBitmap(grayscale, 0, 0, paint2);
        grayscale.recycle();

        FileLogger.d(TAG, "Bitmap preprocessed: grayscale + contrast boost applied.");
        return contrasted;
    }

    /**
     * Handles a successful OCR extraction result.
     *
     * <p>If text was found, shows the OCR result panel with the extracted text.
     * If no text was detected, shows a Toast and keeps the panel hidden.</p>
     *
     * @param extractedText the raw text extracted by ML Kit; may be empty
     */
    private void handleOcrSuccess(@NonNull String extractedText) {
        if (extractedText.isEmpty()) {
            showToast("No text detected.");
            return;
        }

        getBinding().tvOcrResult.setText(extractedText);

        // Show bottom sheet via BottomSheetBehavior
        getBinding().layoutOcrResult.setVisibility(View.VISIBLE);
        final com.google.android.material.bottomsheet.BottomSheetBehavior<LinearLayout> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                        getBinding().layoutOcrResult);
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Switches to the Text tab and pre-fills {@link TranslateFragment}
     * with the OCR-extracted text.
     *
     * @param text the OCR text to send to TranslateFragment
     */
    private void sendToTranslate(@NonNull String text) {
        if (getActivity() == null) return;

        final MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.switchToTextTab();

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (getActivity() == null) return;
            final androidx.fragment.app.Fragment fragment =
                    getActivity().getSupportFragmentManager()
                            .findFragmentByTag(MainActivity.TAG_TRANSLATE);
            if (fragment instanceof TranslateFragment) {
                ((TranslateFragment) fragment).restoreFromOcr(text);
                FileLogger.d(TAG, "OCR text sent to TranslateFragment: " + text);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Language bar
    // -------------------------------------------------------------------------

    /**
     * Swaps the source and target language label text in the top language bar.
     */
    private void swapLanguageLabels() {
        final String tmpCode = sourceLangCode;
        sourceLangCode = targetLangCode;
        targetLangCode = tmpCode;
        getBinding().tvSourceLang.setText(sourceLangCode.toUpperCase());
        getBinding().tvTargetLang.setText(targetLangCode.toUpperCase());
        FileLogger.d(TAG, "Language swapped: " + sourceLangCode + " ↔ " + targetLangCode);
    }

    // -------------------------------------------------------------------------
    // Permission helper
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the CAMERA permission has already been granted.
     *
     * @return {@code true} if permission is granted, {@code false} otherwise
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}