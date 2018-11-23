package com.phoobobo.rnwebviewplus;

import android.content.Intent;

/**
 * Copyright (C) 2018, solo.com, Inc.
 * All Rights Reserved.
 * Created by solo on 2018/11/22.
 * Email ikaroschobits@gmail.com
 */
public interface ActivityResultInterface {
    void callback(int requestCode, int resultCode, Intent data);
}
