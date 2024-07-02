package com.itssvkv.chatapp.ui.splashimport android.content.Contextimport android.os.Handlerimport android.os.Looperimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.itssvkv.chatapp.data.local.repository.SharedPrefRepositoryimport com.itssvkv.chatapp.utils.sharedpref.SharedPrefCommonimport com.itssvkv.chatapp.utils.sharedpref.SharedPrefCommon.IS_USERimport dagger.hilt.android.lifecycle.HiltViewModelimport kotlinx.coroutines.launchimport javax.inject.Inject@HiltViewModelclass SplashViewModel @Inject constructor(    private val repo: SharedPrefRepository) : ViewModel() {    private val _isFirstTimeLiveData = MutableLiveData<Boolean>()    val isFirstTimeLiveData: LiveData<Boolean>        get() = _isFirstTimeLiveData    private val _isUserLiveData = MutableLiveData<Boolean>()    val isUserLiveData: LiveData<Boolean>        get() = _isUserLiveData    fun init(context: Context) {        Handler(Looper.getMainLooper()).postDelayed({            viewModelScope.launch {                val isFirstTime =                    repo.getFromPref(                        context = context,                        key = SharedPrefCommon.IS_FIRST_TIME,                        defValue = true                    ) as Boolean                val isUser =                    repo.getFromPref(                        context = context,                        key = IS_USER,                        defValue = false                    ) as Boolean                if (isFirstTime) {                    repo.saveToPref(                        context = context,                        key = SharedPrefCommon.IS_FIRST_TIME,                        value = false                    )                    _isFirstTimeLiveData.postValue(true)                } else {                    _isFirstTimeLiveData.postValue(false)                    if (isUser) {                        _isUserLiveData.postValue(true)                    } else {                        _isUserLiveData.postValue(false)                    }                }            }        }, 1000L)    }}