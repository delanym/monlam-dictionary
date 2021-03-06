package org.lobsangmonlam.dictionary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.pavelsikun.vintagechroma.ChromaDialog;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;

import org.ironrabbit.type.CustomTypefaceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DrawingActivity extends AppCompatActivity {

    private CanvasView mCanvas;

    private StringBuffer mText;
    private Bitmap mBitmap;

    private File mOutFile;
    private File photoFile = null;

    private static String lastBitmapPath = null;
    private static int lastTextColor = -1;
    private static int lastBackgroundColor = -1;

    private final static int requestIdPhoto = 1234;
    private final static int requestIdCamera = requestIdPhoto +1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

    }

    private void initUI ()
    {
        setContentView(R.layout.activity_drawing);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setCollapsible(true);

        setTitle("");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCanvas = (CanvasView) findViewById(R.id.canvas);

        String word = getIntent().getStringExtra("word");
        String meaning = getIntent().getStringExtra("meaning");

        if (meaning.indexOf(CanvasView.DELIM_TIBETAN)==-1)
            mCanvas.setDelimeter(" ");

        mText = new StringBuffer();
        mText.append(word.trim());
        mText.append(" ");
        mText.append(meaning.trim());

        mCanvas.setFontFamily(CustomTypefaceManager.getCurrentTypeface(this));
        mCanvas.setMode(CanvasView.Mode.TEXT);

        mCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP :
                        mShareActionProvider.setShareIntent(createShareIntent());
                        break;
                    default :
                        break;
                }


                return false;
            }
        });


        mCanvas.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //At this point the layout is complete and the
                //dimensions of myView and any child views are known.
                if (lastBitmapPath != null)
                    loadBitmap(lastBitmapPath);

                if (lastBackgroundColor != -1)
                    mCanvas.setBaseColor(lastBackgroundColor);

                if (lastTextColor != -1)
                    mCanvas.setPaintStrokeColor(lastTextColor);

                mCanvas.setPosition(12,100);
                mCanvas.setText(mText.toString());


            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_photo:
                addPhoto();
                return true;
            case R.id.action_camera:
                takePicture();
                return true;
            case R.id.action_color_text:
                setTextColor();
                return true;
            case R.id.action_color_fill:
                setFillColor();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drawing, menu);
        // Get the menu item.
        MenuItem menuItem = menu.findItem(R.id.action_share);
        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        // Set share Intent.
        // Note: You can set the share Intent afterwords if you don't want to set it right now.
        mShareActionProvider.setShareIntent(createShareIntent());
        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {


                return false;
            }
        });
        return true;
    }


    // Create and return the Share Intent
    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        try {
            Bitmap bitmap = mCanvas.getBitmap();
            File outFolder = new File(Environment.getExternalStorageDirectory(), MonlamConstants.DB_FOLDER_NAME);
            mOutFile = new File(outFolder, new java.util.Date().getTime() + ".jpg");
            FileOutputStream fos = new FileOutputStream(mOutFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mOutFile));
            fos.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return shareIntent;
    }

    @Override
    protected void onResume() {
        super.onResume();



    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private void takePicture ()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            /**
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "org.lobsangmonlam.dictionary.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);


            }**/

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));

            startActivityForResult(takePictureIntent, requestIdCamera);

        }
    }

    private void addPhoto ()
    {

        // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        Intent intent;

        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        //String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
        // Apply is async and fine for UI thread. commit() is synchronous
        //mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Constants.PREFS_CALLING_CARD_ID, cardMediaId).apply();
        startActivityForResult(intent, requestIdPhoto);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String mimeType = null;

        if (requestCode == requestIdCamera) {

            if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                lastBitmapPath = photoFile.getAbsolutePath();
                loadBitmap(lastBitmapPath);
            }
            else if (intent != null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    lastBitmapPath = Utility.getRealPathFromURI(this, uri);
                    loadBitmap(lastBitmapPath);
                }
            }

        }
        else if (requestCode == requestIdPhoto) {
            if (intent != null) {
                Uri uri = intent.getData();
                mimeType = getContentResolver().getType(uri);

                // Will only allow stream-based access to files

                try {
                    if (uri.getScheme().equals("content") && Build.VERSION.SDK_INT >= 19) {
                        grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                } catch (SecurityException se) {
                    Log.d("OA", "security exception accessing URI", se);
                }

                lastBitmapPath = Utility.getRealPathFromURI(this, uri);
                loadBitmap(lastBitmapPath);


            }

            /**
            if (resultCode == RESULT_OK) {


                if (null == lastBitmapPath) {
                    Log.d(TAG, "onActivityResult: Invalid file on import or capture");
                    Toast.makeText(getApplicationContext(), "file not found", Toast.LENGTH_SHORT).show();
                } else if (null == mimeType) {
                    Log.d(TAG, "onActivityResult: Invalid Media Type");
                    Toast.makeText(getApplicationContext(), "invalid media", Toast.LENGTH_SHORT).show();
                } else {


                }
            }*/
        }
    }

    private void loadBitmap (String path)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;

        int reqWidth = mCanvas.getWidth();
        int reqHeight = mCanvas.getHeight();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        mBitmap = BitmapFactory.decodeFile(path, options);

        fillBitmap(mBitmap);

        if (mShareActionProvider != null)
         mShareActionProvider.setShareIntent(createShareIntent());

    }

    private void fillBitmap (Bitmap originalImage)
    {
        int width = mCanvas.getWidth();
        int height = mCanvas.getHeight();
        float originalWidth = originalImage.getWidth(), originalHeight = originalImage.getHeight();
        float scale = width/originalWidth;
        float xTranslation = 0.0f, yTranslation = (height - originalHeight * scale)/2.0f;
        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);
        mCanvas.drawBitmap(originalImage, transformation);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height;
            final int halfWidth = width;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void setTextColor ()
    {


        boolean useOlder = android.os.Build.VERSION.SDK_INT < 14;

        if (useOlder) {
            new ChromaDialog.Builder()
                    .initialColor(mCanvas.getPaintStrokeColor())
                    .colorMode(ColorMode.ARGB) // RGB, ARGB, HVS, CMYK, CMYK255, HSL
                    .indicatorMode(IndicatorMode.DECIMAL) //HEX or DECIMAL; Note that (HSV || HSL || CMYK) && IndicatorMode.HEX is a bad idea
                    .onColorSelected(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(@ColorInt int color) {
                            lastTextColor = color;
                            mCanvas.setPaintStrokeColor(color);
                            mShareActionProvider.setShareIntent(createShareIntent());

                        }
                    })
                    .create()
                    .show(getSupportFragmentManager(), "ChromaDialog");
        }
        else
        {
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("")
                    .initialColor(mCanvas.getPaintStrokeColor())
                    .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                    .density(12)
                    .setPositiveButton(getString(android.R.string.ok), new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            lastTextColor = selectedColor;
                            mCanvas.setPaintStrokeColor(selectedColor);
                            mShareActionProvider.setShareIntent(createShareIntent());                        }
                    })
                    .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .build()
                    .show();
        }


    }

    private void setFillColor ()
    {
        boolean useOlder = android.os.Build.VERSION.SDK_INT < 14;

        if (useOlder) {
            new ChromaDialog.Builder()
                    .initialColor(mCanvas.getBaseColor())
                    .colorMode(ColorMode.ARGB) // RGB, ARGB, HVS, CMYK, CMYK255, HSL
                    .indicatorMode(IndicatorMode.DECIMAL) //HEX or DECIMAL; Note that (HSV || HSL || CMYK) && IndicatorMode.HEX is a bad idea
                    .onColorSelected(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(@ColorInt int color) {
                            lastBackgroundColor = color;
                            mCanvas.clearBitmap();
                            mCanvas.setBaseColor(color);
                            mShareActionProvider.setShareIntent(createShareIntent());


                        }
                    })
                    .create()
                    .show(getSupportFragmentManager(), "ChromaDialog");
        }
        else
        {
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("")
                    .initialColor(mCanvas.getBaseColor())
                    .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                    .density(12)
                    .setPositiveButton(getString(android.R.string.ok), new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            lastBackgroundColor = selectedColor;
                            mCanvas.clearBitmap();
                            mCanvas.setBaseColor(selectedColor);
                            mShareActionProvider.setShareIntent(createShareIntent());
                        }
                    })
                    .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .build()
                    .show();
        }


    }

    private final static String TAG = "monlam";
}
