'use strict';

angular.module('uiApp')
    .controller('LoginCtrl', function ($scope, socketService, $location) {
        //$scope.login.host = "localhost"

        $scope.login = function () {
            var host = $scope.login.host
            var msg = {"command": "connect", payload: {"contactPoints": host.split(",")}};

            socketService.sendMessage(msg).then(
                function (message) {
                    console.log("SUCCESS")
                    console.log(message)
                    $scope.login.response = message
                    if(message.status == "success"){
                        $location.path("/connected")
                    }
                },
                function (message) {
                    console.log("Message sending failed...")

                }
            )
        }
    });
