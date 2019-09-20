package com.toffeegryphon.solartime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SubsolarWallpaperService extends WallpaperService {

    @Override
    public SubsolarEngine onCreateEngine() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        Log.d("METRICS", width + ", " + height);

        return new SubsolarEngine(new Point(width, height));
    }

    public class SubsolarEngine extends Engine implements OnRetrievedBitmapListener {
        private Point metrics; //TODO change this to sharedPrefs
        private int bitmapLength = 512; //TODO sharedPrefs
        private Bitmap.Config config = Bitmap.Config.ARGB_8888;
        private SparseArray<Bitmap> columns = new SparseArray<>();
        private Bitmap currentColumn;
        private int currentColumnIndex;
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        SubsolarEngine(Point metrics) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SubsolarWallpaperService.this);
            this.metrics = metrics;
            handler.post(drawRunner);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawRunner);
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    Subsolar.Coordinate coordinate = Subsolar.equationOfTime();

//                    new RetrieveBitmapTask(this).execute("https://api.mapbox.com/styles/v1/mapbox/satellite-v9/static/" + coordinate.longitude + "," + coordinate.latitude + ",8,0/" + metrics.x + "x" + 1280 + "?access_token=pk.eyJ1IjoidG9mZmVlZ3J5cGhvbiIsImEiOiJjazBxajF1Mm4wOGRoM21tc2UyNmRlYWo0In0.r5yVFHioIJHF4FYdXypWNA");
//                    if (bitmap != null) {
//                        canvas.drawBitmap(bitmap, null, new Rect(0, 0, metrics.x, 1280), null);
//                    }

                    //TODO double confirm if I got the right tile
                    double x = (coordinate.longitude + 180.0) / 2.8125 + 1.0;
                    double y = (coordinate.latitude + 85.0511) / 1.3289234375 + 1.0;
                    Point tile = new Point((int) x, (int) y);
                    double dX = x - tile.x;
                    double dY = y - tile.y;
                    Log.d("TILE", x + ", " + y);
                    Log.d("DELTA", dX + ", " + dY);
                    String tileUrl = String.format(Locale.US,"tile_%d_%d", tile.x, tile.y);
                    Log.d("URL", tileUrl);
                    int pX = (int) (metrics.x / 2 - dX * bitmapLength);
                    int pY = (int) (metrics.y / 2 - dY * bitmapLength);
                    Log.d("POS", pX + ", " + pY);

                    int nV = metrics.y / bitmapLength + 2;
                    int firstY = (int) (y - nV / 2);

                    pY = pY - (tile.y - firstY) * bitmapLength;

                    Paint refresh = new Paint();
                    refresh.setColor(Color.BLACK);
                    refresh.setStyle(Paint.Style.FILL);
                    canvas.drawRect(0, 0, metrics.x, metrics.y, refresh);

                    Typeface dense = Typeface.createFromAsset(getAssets(), "dense.otf");
                    Typeface bold = Typeface.create(dense, Typeface.BOLD);

                    TextPaint text = new TextPaint();
                    text.setTypeface(bold);
                    text.setColor(Color.WHITE);
                    text.setStyle(Paint.Style.FILL);
                    text.setTextSize(30);

                    // TODO Improve

                    Log.d("PY", String.valueOf(pY));
                    if (currentColumn == null) { // If null generate more
                        currentColumn = generateColumn(nV, firstY, tile.x);
                        columns.append(tile.x, currentColumn);
                        currentColumnIndex = tile.x;
                        canvas.drawBitmap(currentColumn, pX, pY, null);
                    }

                    Log.d("PY EAST", String.valueOf(pY));
                    if (tile.x != currentColumnIndex) {
                        // Should totally change to sparsearray. So dont need to move all indices
                        Bitmap column = columns.get(tile.x, null);
                        if (column == null) {
                            currentColumn = generateColumn(nV, firstY, tile.x);
                            columns.append(tile.x, currentColumn);
                            currentColumnIndex = tile.x;
                            canvas.drawBitmap(currentColumn, pX, pY, null);
                        } else {
                            currentColumn = columns.get(tile.x);
                            canvas.drawBitmap(currentColumn, pX, pY, null);
                        }
                    } else {
                        canvas.drawBitmap(currentColumn, pX, pY, null);
                    }
                    Log.d("COLUMNS", columns.toString());

                    Log.d("PY", String.valueOf(pY));
                    int left = pX + bitmapLength;
                    int east = tile.x + 1;
                    while (left + bitmapLength < metrics.y) {
                        Bitmap eastBitmap = columns.get(east, null);
                        if (eastBitmap == null) {
                            eastBitmap = generateColumn(nV, firstY, east);
                            columns.append(east, eastBitmap);
                        }
                        canvas.drawBitmap(eastBitmap, left, pY, null);
                        left += bitmapLength;
                        east += 1;
                    }

                    Log.d("PY WEST", String.valueOf(pY));
                    left = pX - bitmapLength;
                    int west = tile.x - 1;
                    while (left > - bitmapLength) {
                        Bitmap westBitmap = columns.get(west, null);
                        if (westBitmap == null) {
                            westBitmap = generateColumn(nV, firstY, west);
                            columns.append(west, westBitmap);
                            columns.remove(east);
                        }
                        canvas.drawBitmap(westBitmap, left, pY, null);
                        left -= bitmapLength;
                        west -= 1;
                    }

                    text.setTextSize(100);
                    refresh.setColor(Color.YELLOW);
                    canvas.drawCircle(metrics.x / 2, metrics.y / 2, 50, refresh);

                    //TODO allow user to add image as their sun haha so that their SO can be their sun

                    //Nearest City, Country
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1);
                        Log.d("ADDRESSES", addresses.toString());
                        String city = addresses.get(0).getLocality();
                        String country = addresses.get(0).getCountryName();
                        canvas.drawText(String.format("%s, %s", city, country), metrics.x / 16, metrics.y * 5/8, text);
                    } catch (IOException e) {
                        e.printStackTrace();
                        canvas.drawText("Ocean", metrics.x / 8, metrics.y / 2, text);
                    }

                    // Draw Lat, Long
                    text.setTypeface(dense);
                    text.setTextSize(40);
                    DecimalFormat format = new DecimalFormat("###.00");
                    canvas.drawText(String.format("%s, %s", format.format(coordinate.latitude), format.format(coordinate.longitude)), metrics.x / 8, metrics.y / 2 + 40, text);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            handler.removeCallbacks(drawRunner);
            if (isVisible()) {
                handler.postDelayed(drawRunner, 300);
            }
        }

        /*
        nV = number of vertical tiles to stack
        firstY = tileIndex.y of first tile
        tileX = tileIndex.x of column
         */

        Bitmap generateColumn(int nV, int firstY, int tileX) {
            Bitmap column = Bitmap.createBitmap(bitmapLength, nV * bitmapLength, config);
            Canvas columnCanvas = new Canvas(column);
            for (int j = firstY; j <= firstY + nV; j++) {
                String tileId = String.format(Locale.US,"tile_%d_%d", tileX, j);
                Log.d("ID", tileId);
                int resId = getResources().getIdentifier(tileId, "drawable", getPackageName());
                Bitmap tileBitmap = ((BitmapDrawable) Objects.requireNonNull(getDrawable(resId))).getBitmap();
                columnCanvas.drawBitmap(tileBitmap, null, new Rect(0, (j - firstY) * bitmapLength, bitmapLength, (j - firstY) * bitmapLength + bitmapLength), null);

                // Label each tile
//                Paint paint = new Paint();
//                paint.setColor(Color.WHITE);
//                paint.setStyle(Paint.Style.FILL);
//                paint.setTextSize(40);
//                columnCanvas.drawText(String.valueOf(j), 0, (j - firstY) * bitmapLength, paint);
            }
            return column;
        }

        @Override
        public void onBitmapRetrieved(Bitmap bitmap) {
        }
    }

    static class RetrieveBitmapTask extends AsyncTask<String, Void, Bitmap> {

        private SubsolarEngine engine;

        RetrieveBitmapTask(SubsolarEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            engine.onBitmapRetrieved(bitmap);
        }
    }

    interface OnRetrievedBitmapListener {
        void onBitmapRetrieved(Bitmap bitmap);
    }
}
