package com.rock.tgplay.tdlib.manager;

import android.content.Context;
import android.util.Log;

import com.rock.tgplay.tdlib.model.TgAppCredentials;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CredentialsManager {

    String credFilePath;
    private String TAG = "Credentials";

    public CredentialsManager(Context context) {
        credFilePath = context.getFilesDir().getAbsolutePath() + "/credentials";
    }

    public TgAppCredentials getSavedCredentials() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(credFilePath))) {
            Object obj = objectInputStream.readObject();
            if (obj instanceof TgAppCredentials) {
                objectInputStream.close();
                return (TgAppCredentials) obj;
            } else {
                System.err.println("Warning: Object read from file is not of type TgAppCredentials.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error reading credentials from file: " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found while deserializing credentials: " + e.getMessage());
            return null;
        }
    }

    public void saveCredentials(TgAppCredentials credentials){
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(credFilePath))) {
            objectOutputStream.writeObject(credentials);
            Log.d(TAG, "saveCredentials() returned: Saved");
        } catch (IOException ignored) {
            Log.d(TAG, "saveCredentials() returned: Error" + ignored.getMessage());
        }
    }
}
