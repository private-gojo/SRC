package com.ace.injector.util;

import android.content.Context;
import android.os.AsyncTask;

public class Worker2 extends AsyncTask<Void, Integer, String> {
    private WorkerAction workerAction;

    protected void onPreExecute() {
    }

    protected void onProgressUpdate(Integer... numArr) {
    }

    public Worker2(WorkerAction workerAction, Context context) {
        this.workerAction = workerAction;
    }

    protected String doInBackground(Void... voidArr) {
        this.workerAction.runFirst();
        return (String) null;
    }

    protected void onPostExecute(String str) {
        this.workerAction.runLast();
    }
}
