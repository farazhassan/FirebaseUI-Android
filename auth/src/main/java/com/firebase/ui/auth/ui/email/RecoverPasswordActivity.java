/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.email;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.ui.ImeHelper;
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.email.RecoverPasswordHandler;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import uk.co.chrisjenx.calligraphy.TypefaceUtils;

/**
 * Activity to initiate the "forgot password" flow by asking for the user's email.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecoverPasswordActivity extends AppCompatBase implements View.OnClickListener,
        ImeHelper.DonePressedListener {
    private RecoverPasswordHandler mHandler;

    private TextInputLayout mEmailInputLayout;
    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;

    public static Intent createIntent(Context context, FlowParameters params, String email) {
        return createBaseIntent(context, RecoverPasswordActivity.class, params)
                .putExtra(ExtraConstants.EMAIL, email);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_forgot_password_layout);

        // Hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mHandler = ViewModelProviders.of(this).get(RecoverPasswordHandler.class);
        mHandler.init(getFlowParams());
        mHandler.getOperation().observe(this, new ResourceObserver<String>(
                this, R.string.fui_progress_dialog_sending) {
            @Override
            protected void onSuccess(@NonNull String email) {
                mEmailInputLayout.setError(null);
                showEmailSentDialog(email);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthInvalidUserException
                        || e instanceof FirebaseAuthInvalidCredentialsException) {
                    // No FirebaseUser exists with this email address, show error.
                    mEmailInputLayout.setError(getString(R.string.fui_error_email_does_not_exist));
                } else {
                    // Unknown error
                    mEmailInputLayout.setError(getString(R.string.fui_error_unknown));
                }
            }
        });

        mEmailInputLayout = findViewById(R.id.email_layout);
        mEmailEditText = findViewById(R.id.email);
        mEmailFieldValidator = new EmailFieldValidator(mEmailInputLayout);

        mEmailInputLayout.setTypeface(TypefaceUtils.load(getAssets(), "fonts/Raleway-Regular.otf"));
        String email = getIntent().getStringExtra(ExtraConstants.EMAIL);
        if (email != null) {
            mEmailEditText.setText(email);
        }

        ImeHelper.setImeOnDoneListener(mEmailEditText, this);
        findViewById(R.id.button_done).setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_done
                && mEmailFieldValidator.validate(mEmailEditText.getText())) {
            onDonePressed();
        }
    }

    @Override
    public void onDonePressed() {
        mHandler.startReset(mEmailEditText.getText().toString());
    }

    private void showEmailSentDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.fui_title_confirm_recover_password)
                .setMessage(getString(R.string.fui_confirm_recovery_body, email))
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish(RESULT_OK, new Intent());
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
