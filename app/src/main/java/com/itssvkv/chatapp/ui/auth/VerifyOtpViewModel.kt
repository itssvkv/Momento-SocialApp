package com.itssvkv.chatapp.ui.authimport android.app.Activityimport android.content.Contextimport android.util.Logimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.google.firebase.FirebaseExceptionimport com.google.firebase.FirebaseTooManyRequestsExceptionimport com.google.firebase.auth.FirebaseAuthimport com.google.firebase.auth.FirebaseAuthInvalidCredentialsExceptionimport com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaExceptionimport com.google.firebase.auth.PhoneAuthCredentialimport com.google.firebase.auth.PhoneAuthOptionsimport com.google.firebase.auth.PhoneAuthProviderimport com.google.gson.Gsonimport com.itssvkv.chatapp.data.local.repository.FirebaseRepositoryimport com.itssvkv.chatapp.data.local.repository.SharedPrefRepositoryimport com.itssvkv.chatapp.models.UserDataInfoimport com.itssvkv.chatapp.utils.Common.TAGimport com.itssvkv.chatapp.utils.sharedpref.SharedPrefCommonimport dagger.hilt.android.lifecycle.HiltViewModelimport kotlinx.coroutines.launchimport java.util.concurrent.TimeUnitimport javax.inject.Inject@HiltViewModelclass VerifyOtpViewModel @Inject constructor(    private val auth: FirebaseAuth,    private val firebaseRepository: FirebaseRepository,    private val sharedPrefRepository: SharedPrefRepository) : ViewModel() {    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks    private var storedVerificationId = ""    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken    private lateinit var credential: PhoneAuthCredential    var isProgressTrue: (() -> Unit)? = null    var isProgressFalse: (() -> Unit)? = null    var showToast: ((String) -> Unit)? = null    private val _isUserExist = MutableLiveData<Boolean>()    val isUserExist: LiveData<Boolean>        get() = _isUserExist    fun sendMessage(activity: Activity, phoneNumber: String?) {        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {            override fun onVerificationCompleted(credential: PhoneAuthCredential) {                Log.d(TAG, "onVerificationCompleted: $credential")                setInProgress(false)            }            override fun onVerificationFailed(e: FirebaseException) {                when (e) {                    is FirebaseAuthMissingActivityForRecaptchaException -> {                        // reCAPTCHA verification attempted with null Activity                        Log.d(TAG, "onVerificationFailed: ${e.message}")                    }                    is FirebaseAuthInvalidCredentialsException -> {                        // Invalid request                        Log.d(TAG, "onVerificationFailed: ${e.message}")                    }                    is FirebaseTooManyRequestsException -> {                        // The SMS quota for the project has been exceeded                        Log.d(TAG, "onVerificationFailed: ${e.message}")                    }                }                setInProgress(false)            }            override fun onCodeSent(                verificationId: String,                token: PhoneAuthProvider.ForceResendingToken,            ) {                storedVerificationId = verificationId                resendToken = token                setInProgress(false)            }        }        setInProgress(isProgress = true)        val options = PhoneAuthOptions.newBuilder(auth)            .setPhoneNumber(phoneNumber!!) // Phone number to verify            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit            .setActivity(activity) // Activity (for callback binding)            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks            .build()        PhoneAuthProvider.verifyPhoneNumber(options)    }    private fun setInProgress(isProgress: Boolean) {        if (isProgress) {            isProgressTrue?.invoke()        } else {            isProgressFalse?.invoke()        }    }    fun verifyOtp(userOtp: String, context: Context) {        credential = PhoneAuthProvider.getCredential(storedVerificationId, userOtp)        auth.signInWithCredential(credential).addOnCompleteListener { result ->            setInProgress(false)            if (result.isSuccessful) {                Log.d(TAG, "verifyOtp: $storedVerificationId")                viewModelScope.launch {                    firebaseRepository.currentUserDetails().get().addOnSuccessListener {                        Log.d(TAG, "verifyOtp: ${it.toObject(UserDataInfo::class.java)}")                        val userInfo = it.toObject(UserDataInfo::class.java)                        if (userInfo != null) {                            viewModelScope.launch {                                sharedPrefRepository.saveToPref(                                    context = context,                                    key = SharedPrefCommon.CURRENT_USER_INFO,                                    value = Gson().toJson(userInfo)                                )                                sharedPrefRepository.saveToPref(                                    context = context,                                    key = SharedPrefCommon.IS_USER,                                    value = true                                )                                sharedPrefRepository.saveToPref(                                    context = context,                                    key = SharedPrefCommon.CURRENT_USER_ID,                                    value = userInfo.id                                )                            }                            _isUserExist.postValue(true)                        } else {                            _isUserExist.postValue(false)                        }                    }.addOnFailureListener {                        _isUserExist.postValue(false)                    }                }            } else {                showToast?.invoke("otp is invalid")            }        }    }}