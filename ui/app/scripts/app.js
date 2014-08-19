/*global angular*/
/**
 * @ngdoc overview
 * @name uiApp
 * @description
 * # uiApp
 *
 * Main module of the application.
 */
angular
  .module('uiApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ngGrid',
    'ui.bootstrap'
  ])
  .config(function ($routeProvider) {
    'use strict';
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
