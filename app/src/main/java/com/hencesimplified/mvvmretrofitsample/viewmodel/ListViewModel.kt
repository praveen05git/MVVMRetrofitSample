package com.hencesimplified.mvvmretrofitsample.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.hencesimplified.mvvmretrofitsample.di.AppModule
//import com.hencesimplified.mvvmretrofitsample.di.CONTEXT_APP
import com.hencesimplified.mvvmretrofitsample.di.DaggerViewModelComponent
//import com.hencesimplified.mvvmretrofitsample.di.TypeOfContext
import com.hencesimplified.mvvmretrofitsample.model.Animal
import com.hencesimplified.mvvmretrofitsample.model.AnimalApiService
import com.hencesimplified.mvvmretrofitsample.model.ApiKey
import com.hencesimplified.mvvmretrofitsample.util.SharedPreferencesHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.http.Field
import javax.inject.Inject

class ListViewModel(application: Application) : AndroidViewModel(application) {

    val animals by lazy { MutableLiveData<List<Animal>>() }
    val loadError by lazy { MutableLiveData<Boolean>() }
    val loading by lazy { MutableLiveData<Boolean>() }

    private val disposable = CompositeDisposable()

    /*
    Before DI
    private val apiService = AnimalApiService()
    private val prefs = SharedPreferencesHelper(getApplication())
     */

    @Inject
    lateinit var apiService: AnimalApiService //DI

    @Inject
    //@field:TypeOfContext(CONTEXT_APP) //Choosing the context
    lateinit var prefs: SharedPreferencesHelper //DI

    private var invalidApiKey = false

    /* Since PrefsModule needs an argument to be passed this init can't be used
    init {
        DaggerViewModelComponent.create().injectAll(this)
    }
     */

    init {
        DaggerViewModelComponent.builder()
            .appModule(AppModule(getApplication()))
            .build()
            .injectAll(this)
    }

    fun refresh() {
        loading.value = true
        invalidApiKey = false
        val key = prefs.getApiKey()
        if (key.isNullOrEmpty()) {
            getKey()
        } else {
            getAnimals(key)
        }
    }

    fun hardRefresh() {
        loading.value = true
        getKey()
    }

    private fun getKey() {
        disposable.add(
            apiService.getApiKey()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<ApiKey>() {
                    override fun onSuccess(apiKey: ApiKey) {
                        if (apiKey.key.isNullOrEmpty()) {
                            loadError.value = true
                            loading.value = false
                        } else {
                            prefs.saveApiKey(apiKey.key)
                            getAnimals(apiKey.key)
                        }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        loadError.value = true
                        loading.value = false
                    }

                })
        )
    }

    private fun getAnimals(key: String) {
        disposable.add(
            apiService.getAnimals(key)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Animal>>() {
                    override fun onSuccess(animalList: List<Animal>) {
                        animals.value = animalList
                        loadError.value = false
                        loading.value = false
                    }

                    override fun onError(e: Throwable) {
                        if (!invalidApiKey) {
                            invalidApiKey = true
                            getKey()
                        } else {
                            e.printStackTrace()
                            animals.value = null
                            loadError.value = true
                            loading.value = false
                        }
                    }

                })
        )
    }

    /*
    //Local hardcoded data
    private fun getAnimals() {
        val a1 = Animal("Dog")
        val a2 = Animal("Cat")

        val animalList: ArrayList<Animal> = arrayListOf(a1, a2)

        animals.value = animalList
        loadError.value = false
        loading.value = false
    }
     */
}