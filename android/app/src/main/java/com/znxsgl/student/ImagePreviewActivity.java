package com.znxsgl.student;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * 聊天图片全屏预览，支持双击/捏合缩放。
 */
public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        String url = getIntent().getStringExtra("image_url");
        PhotoView photoView = findViewById(R.id.pv_image);
        ImageView ivClose = findViewById(R.id.iv_close);

        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.bg_placeholder_image)
                    .error(R.drawable.bg_error_image)
                    .into(photoView);
        }

        ivClose.setOnClickListener(v -> finish());
    }
}
