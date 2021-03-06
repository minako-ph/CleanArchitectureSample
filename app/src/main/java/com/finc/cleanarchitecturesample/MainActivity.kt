package com.finc.cleanarchitecturesample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

// This is a documentations for all the developers who don't get Clean Architecture in Android.
// I'll try to make it easy to understand.
// If you have any questions, feel free to ask.

// All classes are here for clarify.
// There are already some ideas how to divide modules.
// One is layered-architecture division, the other is domain division.
// IMO, the ideal state is including both of them.
// However, if you are not familiar with CleanArchitecture, it would be harder to do at the same time.
// So, this sample is using the layered-architecture one as a first step.
// You can also include all the classes here in same module(app module), but restrictions are really important
// to maintain the source code well.

//
// ui module
//
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // There implementations are getting easier with DI libraries.
        // If you familiar with it, use Koin or Dagger2
        val userLocalDataStore: UserLocalDataStore =
            UserLocalDataStore("Inject your own local data system")
        val userRemoteDataStore: UserRemoteDataStore =
            UserRemoteDataStore("Inject your own http client")
        val userRepository: UserRepository =
            UserRepositoryImpl(userLocalDataStore, userRemoteDataStore)
        val userUseCase: UserUseCase = UserUseCase(userRepository)
        val userViewModel: UserViewModel = UserViewModel(userUseCase)

        // observe results of clicking(saving use data.)
        userViewModel.userSaved.observe(this, Observer {
            if (it) {
                TODO("implementation in success of saving user data")
            } else {
                TODO("implementation in failure of saving user data")
            }
        })

        like_button.setOnClickListener {
            // You need to get User domain object by yourself
            val user = User("id", "first name", "last name")
            userViewModel.click(user)
        }
    }
}

// ViewModel is an intermediate class, which handles with User events and observe that states(results)
// In this case, this class is observing the state whether user data is saved or not.
class UserViewModel(private val userUseCase: UserUseCase) : ViewModel() {

    // There would be called every after user events with #click(id:String) are finished.
    val userSaved: LiveData<Boolean> =
        LiveDataReactiveStreams.fromPublisher(userUseCase.userSaved)

    fun click(user: User) {
        userUseCase.likeUser(user)
    }

    override fun onCleared() {
        userUseCase.dispose()
        super.onCleared()
    }
}

//
// data module
//

// This is Data Transfer Object, which means just an entity to identify an instance.
// Mainly used as de-serialized model. (from json to each platform)
data class UserDTO(val id: String, val firstName: String, val lastName: String)

// I don't really recommend to make UserDataStore interface if you are a beginner.
// TODO localDataStoredSystem should be injected by your own choice. My recommendation is Room.
class UserLocalDataStore(val localDataStoredSystem: Any) {

    fun fetchUser(id: String): Single<UserDTO> {
        TODO("room should be any local data stored system in Android. Room is recommended for RDBS.")
    }

    fun saveUser(user: UserDTO): Completable {
        TODO("you need to write the impl for saving user data in local data stored system.")
    }
}

// TODO http client should be injected by your own choice. My recommendation is Retrofit.
class UserRemoteDataStore(val httpClient: Any) {

    fun fetchUser(id: String): Single<UserDTO> {
        TODO("you need to write api call here with http client")
    }

    fun postUser(user: UserDTO): Completable {
        TODO("you need to write api call here with http client")
    }
}

// This is the intermediate layer from data module(UserDTO) to domain module(User)
class UserRepositoryImpl(
    private val localDataStore: UserLocalDataStore,
    private val remoteDataStore: UserRemoteDataStore
) : UserRepository {

    override fun getUser(id: String): Single<User> {
        // You can load local data if any. If not, you can load user data from remote.
        // After getting data you need to map UserDTO data to User data, which is the domain object
        // that is concerned in that domain.
        return localDataStore.fetchUser(id)
            .concatWith { remoteDataStore.fetchUser(id) }
            .map { User(it.id, it.firstName, it.lastName) }
            .singleOrError()
    }

    override fun saveUser(user: User): Completable {
        val userDTO: UserDTO = UserDTO(user.id, user.firstName, user.lastName)
        return remoteDataStore.postUser(userDTO)
            .andThen(localDataStore.saveUser(userDTO))
    }
}

//
// domain module
//

// User is a domain object, which is not same as UserDTO.
// Suppose domain doesn't have to have id, first name, last name because ui layer just wants to know
// account name, which is combination of first name and last name.
// Thanks to this, you can make the domain clear and testable.
data class User(
    val id: String,
    val firstName: String,
    val lastName: String
) {
    fun getAccoutName() = "$firstName $lastName"
}


// This is just interface used in DOMAIN module
interface UserRepository {
    fun getUser(id: String): Single<User>
    fun saveUser(user: User): Completable
}

// Suppose this is a Matching app like Tinder.
// And you can like or dislike with swipe.
class UserUseCase(private val userRepository: UserRepository) {

    private val compositeDisposable = CompositeDisposable()
    private val userSavedProcessor: BehaviorProcessor<Boolean> = BehaviorProcessor.create()
    val userSaved: Flowable<Boolean> = userSavedProcessor

    // All UseCase functions shouldn't have returning value!!!!!!!
    fun likeUser(user: User) {
        compositeDisposable.add(
            userRepository.saveUser(user)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    userSavedProcessor.onNext(true)
                }, {
                    userSavedProcessor.onNext(false)
                    print(it)
                })
        )
    }

    fun dislikeUser() {
        TODO("same as likeUser()")
    }

    fun dispose() {
        compositeDisposable.dispose()
    }
}
