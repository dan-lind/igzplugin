package com.danlind.igz.handler;

public class LoginExecutor {
//
//    private final Authentification authentification;
//    private final CredentialsFactory credentialsFactory;
//    private final Zorro zorro;
//
//    private final static Logger logger = LoggerFactory.getLogger(LoginExecutor.class);
//
//    public LoginExecutor(final Authentification authentification,
//                        final CredentialsFactory credentialsFactory,
//                        final Zorro zorro) {
//        this.authentification = authentification;
//        this.credentialsFactory = credentialsFactory;
//        this.zorro = zorro;
//    }
//
//    public int login(final String username,
//                     final String password,
//                     final String loginType) {
//        final LoginCredentials credentials = credentialsFactory.create(username,
//                                                                       password,
//                                                                       loginType);
//        return loginWithCredentials(credentials);
//    }
//
//    private int loginWithCredentials(final LoginCredentials credentials) {
//        final Observable<Integer> login = authentification
//            .login(credentials)
//            .andThen(Observable.just(ZorroReturnValues.LOGIN_OK.getValue()))
//            .onErrorResumeNext(err -> {
//                logger.error("Failed to login with exception " + err.getMessage());
//                return Observable.just(ZorroReturnValues.LOGIN_FAIL.getValue());
//            });
//
//        return zorro.progressWait(login);
//    }
//
//    public int logout() {
//        authentification
//            .logout()
//            .blockingAwait();
//        return ZorroReturnValues.LOGOUT_OK.getValue();
//    }
}
