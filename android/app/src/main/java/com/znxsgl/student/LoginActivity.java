package com.znxsgl.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.znxsgl.student.model.LoginRequest;
import com.znxsgl.student.model.LoginResponse;
import com.znxsgl.student.network.ApiService;
import com.znxsgl.student.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化 RetrofitClient 的全局 Context（用于 401 拦截跳转）
        RetrofitClient.init(this);

        // 检查是否已登录
        SharedPreferences prefs = getSharedPreferences("znxsgl", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token != null) {
            // 已有 token，直接跳主页
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        apiService = RetrofitClient.getInstance().create(ApiService.class);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.login_loading);
        progressBar.setVisibility(View.VISIBLE);

        LoginRequest request = new LoginRequest(username, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, retrofit2.Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login_btn);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse data = response.body();
                    if (data.getToken() != null) {
                        // 登录成功，保存 token
                        SharedPreferences prefs = getSharedPreferences("znxsgl", MODE_PRIVATE);
                        prefs.edit()
                                .putString("token", data.getToken())
                                .putString("realName", data.getRealName())
                                .putString("username", data.getUsername())
                                .putLong("userId", data.getUserId())
                                .putInt("role", data.getRole())
                                .apply();

                        Toast.makeText(LoginActivity.this,
                                "欢迎，" + data.getRealName(), Toast.LENGTH_SHORT).show();

                        // 跳转到主页
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                data.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errBody = response.errorBody().string();
                        Toast.makeText(LoginActivity.this, errBody, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, R.string.error_auth, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.login_btn);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this,
                        R.string.error_network + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
