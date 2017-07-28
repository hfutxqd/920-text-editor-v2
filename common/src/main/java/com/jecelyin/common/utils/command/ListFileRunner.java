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

import android.text.TextUtils;

import com.jecelyin.common.utils.FileInfo;
import com.jecelyin.common.utils.L;
import com.jecelyin.common.utils.RootShellRunner;
import com.jecelyin.common.utils.ShellProcessor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Jecelyin Peng <jecelyin@gmail.com>
 */

public class ListFileRunner extends Runner<List<FileInfo>> {
    private final String path;

    public ListFileRunner(String path) {
        this.path = path;
    }

    @Override
    public String command() {
        return "ls -la \"" + path + "\"";
    }

    @Override
    public void onResult(RootShellRunner runner, List<String> results) {
        final List<FileInfo> files = new ArrayList<>();
        eachResults(results, files);
    }

    private void eachResults(List<String> results, List<FileInfo> files) {
        if (results.isEmpty()) {
            onSuccess(files);
            return;
        }
        String line = results.remove(0);
        line = line.trim();
        // lstat '//persist' failed: Permission denied
        if (line.startsWith("lstat \'" + path) && line.contains("\' failed: Permission denied")) {
            line = line.replace("lstat \'" + path, "");
            line = line.replace("\' failed: Permission denied", "");
            if (line.startsWith("/")) {
                line = line.substring(1);
            }
            FileInfo failedToRead = new FileInfo(false, line);
            files.add(failedToRead);
            eachResults(results, files);
            return;
        }
        // /data/data/com.android.shell/files/bugreports: No such file or directory
        if (line.startsWith("/") && line.contains(": No such file")) {
            eachResults(results, files);
            return;
        }
        try {
            lsParser(results, files, path, line);
        } catch (Exception e) {
            L.e("parse line error: " + line, e);
            eachResults(results, files);
        }
    }

    private void lsParser(final List<String> results, final List<FileInfo> files, String path, String line) {
        final String[] split = line.split(" ");
        int index = 0;

        final FileInfo file = new FileInfo(false, "");

        String date = "";
        String time = "";
        //drwxrwx--x 3 root sdcard_rw 4096 2016-12-17 15:02 obb
        for (String token : split) {
            if (token.trim().isEmpty())
                continue;
            switch (index) {
                case 0: {
                    file.permissions = token;
                    break;
                }
                case 1: {
                    if (TextUtils.isDigitsOnly(token))
                        continue;
                    file.owner = token;
                    break;
                }
                case 2: {
                    file.group = token;
                    break;
                }
                case 3: {
                    if (token.contains("-")) {
                        // No length, this is the date
                        file.size = -1;
                        date = token;
                    } else if (token.contains(",")) {
                        //In /dev, ls lists the major and minor device numbers
                        file.size = -2;
                    } else {
                        // Length, this is a file
                        try {
                            file.size = Long.parseLong(token);
                        } catch (Exception e) {
                            throw new NumberFormatException(e.getMessage() + " Line: " + line);
                        }
                    }
                    break;
                }
                case 4: {
                    if (file.size == -1) {
                        // This is the time
                        time = token;
                    } else {
                        // This is the date
                        date = token;
                    }
                    break;
                }
                case 5: {
                    if (file.size == -2) {
                        date = token;
                    } else if (file.size > -1) {
                        time = token;
                    }
                    break;
                }
                case 6:
                    if (file.size == -2) {
                        time = token;
                    }
                    break;
            }
            index++;
        }

        if (line.length() > 0) {
            final String nameAndLink = line.substring(line.indexOf(time) + time.length() + 1);
            if (nameAndLink.contains(" -> ")) {
                final String[] splitSl = nameAndLink.split(" -> ");
                file.name = splitSl[0].trim();
                String realPath = splitSl[1].trim();
                if (realPath.charAt(0) != '/') {
                    file.linkedPath  = new File(path).getParent() + "/" + realPath;
                } else {
                    file.linkedPath  = realPath;
                }
            } else {
                file.name = nameAndLink;
            }
        }

        try {
            file.lastModified = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault())
                    .parse(date + time).getTime();
        } catch (Exception e) {
//            L.e(e); //ignore: java.text.ParseException: Unparseable date: ""
            file.lastModified = 0;
        }

        file.readAvailable = true;
        file.directoryFileCount = "";

        char type = file.permissions.charAt(0);

        if (type == 'd') {
            file.isDirectory = true;
            files.add(file);
            eachResults(results, files);
        } else if (type == 'l') {
            file.isSymlink = true;
            String linkPath = file.linkedPath;
            ShellProcessor.getShell().addCommand(new IsDirectoryRunner(linkPath) {
                @Override
                public void onError(String error) {
                    files.add(file);
                    eachResults(results, files);
                }

                @Override
                public void onSuccess(Boolean result) {
                    file.isDirectory = result;
                    files.add(file);
                    eachResults(results, files);
                }
            });
        } else {
            files.add(file);
            eachResults(results, files);
        }


    }
}
