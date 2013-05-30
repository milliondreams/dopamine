'use strict';

angular.module('uiApp', ['ngGrid'])
    .config(function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'views/main.html'
            })
            .when('/connected', {
                templateUrl: 'views/connected.html',
                controller: 'ConnectedCtrl'
            })
            .when('/casui', {
                templateUrl: 'views/casui.html',
                controller: 'CasuiCtrl'
            })
            .otherwise({
                redirectTo: '/'
            });
    }).run(function ($location, $rootScope) {
        console.log("Inside run . . .");
        if (!($rootScope.connected)) {
            console.log("PATH");
            $location.path("/");
        }
    });
