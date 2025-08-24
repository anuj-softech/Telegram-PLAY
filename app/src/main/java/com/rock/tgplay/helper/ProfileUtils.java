package com.rock.tgplay.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.ImageView;

import com.rock.tgplay.tdlib.TelegramClient;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.File;

import io.reactivex.rxjava3.core.Observable;

public class ProfileUtils {


    public static Observable<File> download(TdApi.File file) {
        Observable<File> observable = Observable.create(emitter -> {
            if (file.local.isDownloadingCompleted) {
                emitter.onNext(new File(file.local.path));
                emitter.onComplete();
            } else {
                Client client = TelegramClient.getInstance().getClient();
                client.send(new TdApi.DownloadFile(file.id, 1, 0, 0, true), object1 -> {
                    if (object1 instanceof TdApi.File) {
                        TdApi.File profile = (TdApi.File) object1;

                        {
                            emitter.onNext(new File(profile.local.path));
                            emitter.onComplete();
                        }
                    }
                });
            }
        });
        return observable;
    }

    public static void loadProfile(ImageView profile, String title) {
        Bitmap bitmap = createBitmapWithLetter(title, 256, 256);
        profile.setImageBitmap(bitmap);
    }

    public static Bitmap createBitmapWithLetter(String text, int width, int height) {
        // Get the first letter of the string and make it uppercase
        char letter = text.toUpperCase().charAt(0);

        // Generate a color based on the letter
        int backgroundColor = generateColorForLetter(letter);

        // Create a bitmap with specified width and height
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Set the background color
        canvas.drawColor(backgroundColor);

        // Prepare paint for drawing the text
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE); // You can set the text color here
        paint.setTextSize(100); // Set text size based on your requirement
        paint.setTextAlign(Paint.Align.CENTER);

        // Measure the text bounds to center it
        Rect bounds = new Rect();
        paint.getTextBounds(String.valueOf(letter), 0, 1, bounds);

        // Calculate the coordinates to draw the text
        int x = width / 2;
        int y = (height / 2) + (bounds.height() / 2);

        // Draw the text on the canvas
        canvas.drawText(String.valueOf(letter), x, y, paint);

        return bitmap;
    }

    public static int generateColorForLetter(char letter) {
        // Hash the letter to a unique color
        int hash = letter * 31;
        int red = (hash & 0xFF0000) >> 16;
        int green = (hash & 0x00FF00) >> 8;
        int blue = hash & 0x0000FF;

        // Ensure the colors are bright enough to be distinct
        red = (red + 128) % 256;
        green = (green + 128) % 256;
        blue = (blue + 128) % 256;

        return Color.rgb(red, green, blue);
    }

}
