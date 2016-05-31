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

package com.firebase.ui.auth.ui.account_link;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;

import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.auth.test_helpers.ActivityHelperShadow;
import com.firebase.ui.auth.test_helpers.CustomRobolectricGradleTestRunner;
import com.firebase.ui.auth.test_helpers.FirebaseAuthWrapperImplShadow;
import com.firebase.ui.auth.test_helpers.GoogleProviderShadow;
import com.firebase.ui.auth.test_helpers.TestConstants;
import com.firebase.ui.auth.test_helpers.TestHelper;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FacebookAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Collections;


@RunWith(CustomRobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {FirebaseAuthWrapperImplShadow.class, GoogleProviderShadow.class},
        sdk = 21)
public class SaveCredentialsActivityTest {

    @Before
    public void setUp() {
        TestHelper.initializeApp(RuntimeEnvironment.application);
    }

    private SaveCredentialsActivity createActivity(
            String name,
            String email,
            String password,
            String provider,
            String profilePictureUrl) {
        Intent saveCredentialsIntent = SaveCredentialsActivity.createIntent(
                RuntimeEnvironment.application,
                TestHelper.getFlowParameters(
                        RuntimeEnvironment.application,
                        Collections.<String>emptyList()),
                name,
                email,
                password,
                provider,
                profilePictureUrl
        );

        return Robolectric
                .buildActivity(SaveCredentialsActivity.class)
                .withIntent(saveCredentialsIntent)
                .create()
                .visible()
                .get();
    }

    @Test
    @Config(shadows = {FirebaseAuthWrapperImplShadow.class, ActivityHelperShadow.class})
    public void testSaveCredentialsApiIsCalled_forIdpAccount() {
        SaveCredentialsActivity saveCredentialsActivity = createActivity(
                TestConstants.NAME,
                TestConstants.EMAIL,
                null,
                FacebookAuthProvider.PROVIDER_ID,
                TestConstants.PHOTO_URL
        );

        PendingResult mockPendingResult = mock(PendingResult.class);
        when(ActivityHelperShadow.credentialsApi.save(
                (GoogleApiClient) anyObject(),
                (Credential)anyObject())).thenReturn(mockPendingResult);

        // pretend the client connected
        saveCredentialsActivity.onConnected(null);

        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);

        // verify that the call to save the credential is made
        verify(ActivityHelperShadow.credentialsApi).save(
                (GoogleApiClient) anyObject(),
                credentialCaptor.capture());

        assertEquals(
                TestConstants.NAME,
                credentialCaptor.getValue().getName());
        assertEquals(
                TestConstants.EMAIL,
                credentialCaptor.getValue().getId());
        assertEquals(
                TestConstants.PHOTO_URI,
                credentialCaptor.getValue().getProfilePictureUri());
        assertEquals(
                IdentityProviders.FACEBOOK,  // translated provider
                credentialCaptor.getValue().getAccountType());
    }

    @Test
    public void testSaveCredentialResult_onSuccess() {
        SaveCredentialsActivity saveCredentialsActivity = createActivity(
                TestConstants.NAME,
                TestConstants.EMAIL,
                null,
                FacebookAuthProvider.PROVIDER_ID,
                TestConstants.PHOTO_URL
        );

        saveCredentialsActivity.onResult(new Status(-1)); // status: success
        ShadowActivity activityShadow = Shadows.shadowOf(saveCredentialsActivity);
        assertTrue(activityShadow.isFinishing());
        assertEquals(
                Activity.RESULT_OK,
                activityShadow.getResultCode());
    }

    @Test
    public void testSaveCredentialResult_onFailure() {
        SaveCredentialsActivity saveCredentialsActivity = createActivity(
                TestConstants.NAME,
                TestConstants.EMAIL,
                null,
                FacebookAuthProvider.PROVIDER_ID,
                TestConstants.PHOTO_URL
        );

        saveCredentialsActivity.onResult(new Status(1)); // status: failure
        ShadowActivity activityShadow = Shadows.shadowOf(saveCredentialsActivity);
        assertTrue(activityShadow.isFinishing());
        assertEquals(
                Activity.RESULT_FIRST_USER,
                activityShadow.getResultCode());
    }
}
