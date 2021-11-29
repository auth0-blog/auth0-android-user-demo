//
// MainActivity.kt
//
// Created by Joey deVilla, November 2021.
// Companion project for the Auth0 blog article
// “Working with User Profile Information in Android Apps”.
//
// See the end of the file for licensing information.
//

package com.example.auth0userdemo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.management.ManagementException
import com.auth0.android.management.UsersAPIClient
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.google.android.material.snackbar.Snackbar
import com.example.auth0userdemo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    // UI controls
    private lateinit var binding: ActivityMainBinding

    // Login/logout-related properties
    private lateinit var account: Auth0
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { login() }
        binding.logoutButton.setOnClickListener { logout() }
        binding.refreshButton.setOnClickListener { getUserProfile() }
        binding.setUserMetadataButton.setOnClickListener { setUserMetadata() }
    }

    private fun login() {
        WebAuthProvider
            .login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withScope(getString(R.string.login_scopes))
            .withAudience(getString(R.string.login_audience, getString(R.string.com_auth0_domain)))
            .start(this, object : Callback<Credentials, AuthenticationException> {

                override fun onFailure(exception: AuthenticationException) {
                    showSnackBar(getString(R.string.login_failure_message, exception.getCode()))
                }

                override fun onSuccess(credentials: Credentials) {
                    cachedCredentials = credentials
                    showSnackBar(getString(R.string.login_success_message, credentials.accessToken))

                    getUserProfile()
                    updateUI()
                }
            })
    }

    private fun getUserProfile() {
        // Guard against showing the profile the user isn’t logged in
        if (cachedCredentials == null) {
            return
        }

        val client = AuthenticationAPIClient(account)
        client
            .userInfo(cachedCredentials!!.accessToken!!)
            .start(object : Callback<UserProfile, AuthenticationException> {

                override fun onFailure(exception: AuthenticationException) {
                    showSnackBar(getString(R.string.general_failure_with_exception_code,
                        exception.getCode()))
                }

                override fun onSuccess(userProfile: UserProfile) {
                    cachedUserProfile = userProfile
                    getUserMetadata()
                    updateUI()
                }

            })
    }

    private fun getUserMetadata() {
        // Guard against getting the metadata when no user is logged in
        if (cachedCredentials == null || cachedUserProfile == null) {
            return
        }

        val usersClient = UsersAPIClient(account, cachedCredentials!!.accessToken!!)

        usersClient
            .getProfile(cachedUserProfile!!.getId()!!)
            .start(object : Callback<UserProfile, ManagementException> {

                override fun onFailure(exception: ManagementException) {
                    showSnackBar(getString(R.string.general_failure_with_exception_code,
                        exception.getCode()))
                }

                override fun onSuccess(userProfile: UserProfile) {
                    cachedUserProfile = userProfile
                    updateUI()
                }

            })
    }

    private fun setUserMetadata() {
        // Guard against getting the metadata when no user is logged in
        if (cachedCredentials == null) {
            return
        }

        val usersClient = UsersAPIClient(account, cachedCredentials!!.accessToken!!)
        val metadata = mapOf(
            "country" to binding.countryEdittext.text.toString().trim(),
            "favorite_color" to binding.favoriteColorEdittext.text.toString().trim()
        )

        usersClient
            .updateMetadata(cachedUserProfile!!.getId()!!, metadata)
            .start(object : Callback<UserProfile, ManagementException> {

                override fun onFailure(exception: ManagementException) {
                    showSnackBar(getString(
                        R.string.general_failure_with_exception_code,
                        exception.getCode()
                    ))
                }

                override fun onSuccess(profile: UserProfile) {
                    cachedUserProfile = profile
                    updateUI()
                    showSnackBar(getString(R.string.general_success_message))
                }

            })
    }

    private fun updateUI() {
        val isLoggedIn = cachedCredentials != null

        binding.textviewTitle.text = if (isLoggedIn) {
            getString(R.string.logged_in_title)
        } else {
            getString(R.string.logged_out_title)
        }
        binding.loginButton.isEnabled = !isLoggedIn
        binding.logoutButton.isEnabled = isLoggedIn

        binding.userInfoLayout.isVisible = isLoggedIn

        updateUserInfoUI()
        updateUserMetadataUI()
        updateAppMetadataUI()

        val id = cachedUserProfile?.getId() ?: "No getId() value"
        Log.d("Auth0", "getId() result: $id")
    }

    private fun updateUserInfoUI() {
        // Guard against displaying user profile info if there is no profile
        if (cachedUserProfile == null) {
            return
        }

        val userName = cachedUserProfile?.name.toString()
        binding.userNameValue.text = userName

        val givenName = cachedUserProfile?.givenName.toString()
        binding.userGivenNameValue.text = givenName

        val familyName = cachedUserProfile?.familyName.toString()
        binding.userFamilyNameValue.text = familyName

        val nickname = cachedUserProfile?.nickname.toString()
        binding.userNicknameValue.text = nickname

        val email = cachedUserProfile?.email.toString()
        binding.userEmailValue.text = email

        val isEmailVerified = cachedUserProfile?.isEmailVerified.toString()
        binding.userIsEmailVerifiedValue.text = isEmailVerified

        val pictureURL = cachedUserProfile?.pictureURL.toString()
        binding.userPictureURLValue.text = pictureURL

        val createdAt = cachedUserProfile?.createdAt.toString()
        binding.userCreatedAtValue.text = createdAt

        val extraInfo = cachedUserProfile?.getExtraInfo().toString()
        binding.userExtraInfoValue.text = extraInfo
    }

    private fun updateUserMetadataUI() {
        // Guard against displaying user metadata if there is no profile
        if (cachedUserProfile == null) {
            return
        }

        val userMetadata = cachedUserProfile?.getUserMetadata().toString()
        binding.userMetadataValue.text = userMetadata

        val country = cachedUserProfile?.getUserMetadata()?.get("country").toString()
        val countryValue = if (country == "null") {
            ""
        } else {
            country
        }
        binding.countryEdittext.setText(countryValue)

        val favoriteColor = cachedUserProfile?.getUserMetadata()?.get("favorite_color").toString()
        val favoriteColorValue = if (favoriteColor == "null") {
            ""
        } else {
            favoriteColor
        }
        binding.favoriteColorEdittext.setText(favoriteColorValue)
    }


    private fun updateAppMetadataUI() {
        // Guard against displaying app metadata if there is no profile
        if (cachedUserProfile == null) {
            return
        }

        val appMetadata = cachedUserProfile?.getAppMetadata().toString()
        binding.appMetadataValue.text = appMetadata
    }

    private fun logout() {
        WebAuthProvider
            .logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Void?, AuthenticationException> {

                override fun onFailure(exception: AuthenticationException) {
                    updateUI()
                    showSnackBar(getString(R.string.general_failure_with_exception_code,
                        exception.getCode()))
                }

                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                    updateUI()
                }

            })
    }

    private fun showSnackBar(text: String) {
        Snackbar
            .make(
            binding.root,
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }

}


//
// License information
// ===================
//
// Copyright (c) 2021 Auth0 (http://auth0.com)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
