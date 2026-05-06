package com.example.vaultify.Login_signup;

import android.annotation.SuppressLint;
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


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;
import com.example.vaultify.R;



public class SignupActivity extends AppCompatActivity {
    EditText email, password;
    Button createAccountBtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.signupEmail);
        password = findViewById(R.id.signupPassword);
        createAccountBtn = findViewById(R.id.createAccountBtn);


        createAccountBtn.setOnClickListener(v -> signUp());
    }
    private void signUp() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        if (userEmail.isEmpty() || userPass.isEmpty()) {
            Toast.makeText(this, "Email & Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("ClientId", CognitoConfig.CLIENT_ID);
            json.put("Username", userEmail);
            json.put("Password", userPass);

            JSONArray attributes = new JSONArray();
            JSONObject emailAttr = new JSONObject();
            emailAttr.put("Name", "email");
            emailAttr.put("Value", userEmail);
            attributes.put(emailAttr);

            json.put("UserAttributes", attributes);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/x-amz-json-1.1")
            );

            Request request = new Request.Builder()
                    .url("https://cognito-idp." + CognitoConfig.REGION + ".amazonaws.com/")
                    .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.SignUp")
                    .addHeader("Content-Type", "application/x-amz-json-1.1")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String responseBody = response.body() != null
                            ? response.body().string()
                            : "";

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(SignupActivity.this,
                                    "Signup success. Check email for OTP",
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, VerifyActivity.class));
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    "Error: " + responseBody,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(SignupActivity.this,
                                    e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}