/*global angular*/

angular.module('uiApp', ['ui.bootstrap', 'ngGrid'])
    .config(function ($routeProvider) {
        "use strict";
        $routeProvider
            .when('/', {
                templateUrl: 'views/main.html',
                controller: 'MainCtrl'
            })
            .when('/workbench', {
                templateUrl: 'views/workbench.html',
                controller: 'WorkbenchCtrl'
            })
            .otherwise({
                redirectTo: '/connect'
            });
    });
