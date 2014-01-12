/*global $,angular,WebSocket*/

angular.module('uiApp')
    .controller('MainCtrl', ['$rootScope', '$scope', '$location', 'WebSocketService', function ($rootScope, $scope, $location, WebSocketService) {
        'use strict';
        var defaultHost = "localhost",
            defaultPort = 9042;

        $scope.host = defaultHost;
        $scope.port = defaultPort;
        $scope.userName = "";
        $scope.password = "";
        $scope.error = "";

        function connectionHandler(data) {
            if (data.status === "Connected") {
                $rootScope.status = "Connected";
                $scope.error = "";
                $location.path("/workbench");
                $scope.$apply();
            } else if (data.status === "Connection Failure") {
                $scope.error = data.msg;
                $scope.$apply();
            }
        }

        $scope.connectToDB = function () {
            var message = {
                    host: defaultHost.split(","),
                    port: defaultPort
                },
                hosts = $scope.host.trim().split(","),
                port = parseInt($scope.port, 10);

            if (hosts.length > 0) {
                if ((hosts.length === 1 && hosts[0] !== defaultHost) || (hosts.length > 1)) {
                    message.host = hosts;
                }
            }
            if (port !== defaultPort) {
                message.port = port;
            }
            if ($scope.userName.trim().length > 0) {
                message.userName = $scope.userName.trim();
                message.password = $scope.password;
            }
            WebSocketService.connect(message);
        };

        WebSocketService.registerHandle(connectionHandler);
    }]);
