'use strict';

angular.module('uiApp', [])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html'
      })
      .when('/connected', {
        templateUrl: 'views/connected.html',
        controller: 'ConnectedCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
