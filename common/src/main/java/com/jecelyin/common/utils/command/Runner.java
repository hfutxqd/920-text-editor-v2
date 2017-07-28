/*
 * Copyright (C) 2016 Jecelyin Peng <jecelyin@gmail.com>
 *
 * This file is part of 920 Text Editor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jecelyin.common.utils.command;

/**
 * @author Jecelyin Peng <jecelyin@gmail.com>
 */

import com.jecelyin.common.listeners.OnResultCallback;
import com.jecelyin.common.utils.L;
import com.jecelyin.common.utils.RootShellRunner;

import java.util.List;
import java.util.UUID;

public abstract class Runner<T> implements OnResultCallback<T> {
    public final String token = UUID.randomUUID().toString();
    abstract public String command();
    abstract public void onResult(RootShellRunner runner, List<String> results);

    @Override
    public void onError(String error) {
        L.d("CMD", "ERROR:" + error);
    }

    @Override
    public void onSuccess(T result) {

    }
}