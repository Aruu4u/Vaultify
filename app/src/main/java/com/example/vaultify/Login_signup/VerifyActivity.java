package com.example.vaultify.Login_signup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vaultify.R;
import com.example.vaultify.R;

import org.json.JSONObject;

import okhttp3.*;
public class VerifyActivity extends AppCompatActivity {

    EditText email, otp;
    Button verifyBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify);

        email = findViewById(R.id.email);
        otp = findViewById(R.id.otp);
        verifyBtn = findViewById(R.id.verifyBtn);

        verifyBtn.setOnClickListener(v -> verifyUser());
    }
    private void verifyUser() {
        String userEmail = email.getText().toString().trim();
        String code = otp.getText().toString().trim();

        if (userEmail.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("ClientId", CognitoConfig.CLIENT_ID);
            json.put("Username", userEmail);
            json.put("ConfirmationCode", code);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/x-amz-json-1.1")
            );

            Request request = new Request.Builder()
                    .url("https://cognito-idp." + CognitoConfig.REGION + ".amazonaws.com/")
                    .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.ConfirmSignUp")
                    .addHeader("Content-Type", "application/x-amz-json-1.1")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String res = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(this, "Account Verified!", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, LoginActivity.class));
                        } else {
                            Toast.makeText(this, "Error: " + res, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}