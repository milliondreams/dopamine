'use strict';

angular.module('uiApp')
    .controller('LoginCtrl', function ($scope, socketService, $location, $rootScope) {
        $scope.login = {host: "localhost"};

        $scope.loginToCas = function () {
            var host = $scope.login.host
            var msg = {"command": "connect", payload: {"contactPoints": host.split(",")}};

            socketService.sendMessage(msg).then(
                function (message) {

                    console.log("SUCCESS");
                    console.log(message);

                    $scope.login.response = message;
                    if (message.status === "success") {
                        $rootScope.connected = true;
                        $location.path("/casui");
                    }
                },
                function (message) {
                    console.log("Message sending failed...");

                }
            )
        }
    });
