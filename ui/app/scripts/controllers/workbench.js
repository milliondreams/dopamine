/*global $,angular,WebSocket*/

angular.module('uiApp')
    .controller('WorkbenchCtrl', ['$rootScope', '$scope', '$location', '$timeout', 'WebSocketService', function ($rootScope, $scope, $location, $timeout, WebSocket) {
        'use strict';

        var queryId = 1,
            isQueryForLeftPanel = false;

        $scope.tabs = [
            { id: 1, title: "Query 1", query: "", result: [], active: true, error: ""},
            { id: 2, title: "Query 2", query: "", result: [], error: ""}
        ];

        $scope.queryTab = [];

        $scope.fetchKeyspaceData = function () {
            isQueryForLeftPanel = true;
            WebSocket.query("select * from system.schema_keyspaces", 0);
        };

        function updateKeyspaces(data) {
            var keyspaces = [];
            _.each(data, function (keyspace) {
                if (keyspace.keyspace_name !== "system" && keyspace.keyspace_name !== "system_traces") {
                    keyspaces.push(keyspace);
                }
            });
            return keyspaces;
        }

        function getResultTab(data) {
            var tabId,
                resultTab = {query: "", result: [], error: ""};

            if (!isQueryForLeftPanel && data.queryId !== undefined) {
                tabId = $scope.queryTab.filter(function (t) {
                    return t.queryId === data.queryId;
                })[0].id;
                resultTab = _.where($scope.tabs, {id: tabId})[0];
            }
            return resultTab
        }

        function getColumnDef(columns) {
            return _.map(columns, function (k) {
                return {field: k, displayName: k}
            });
        }

        function queryResultHandler(data) {
            var resultTab = getResultTab(data);

            switch (data.status) {
                case "QueryResult":
                    if (isQueryForLeftPanel) {
                        $scope.keyspaces = updateKeyspaces(data.payload);
                        isQueryForLeftPanel = false;
                    } else if (data.payload.length > 0) { //do nothing when payload==0
                        resultTab.result = data.payload;
                        resultTab.resultCols = getColumnDef(Object.keys(data.payload[0]));
                    }
                    break;
                case "InvalidQuery":
                    resultTab.error = data.errorMsg;
                    break;
            }
            $scope.$apply();
        }

        $scope.submitQuery = function (id, query) {
            $scope.queryTab.push({id: id, queryId: queryId});
            WebSocket.query(query, queryId);
            queryId += 1;
        };

        function disconnectHandler(data) {
            if (data.status === "Disconnected") {
                $location.path("/");
                $scope.$apply();
            }
        }

        if ($rootScope.status === "Connected") {
            $scope.fetchKeyspaceData();
        }

        $scope.disconnect = function () {
            WebSocket.disconnect();
        };

        WebSocket.registerHandle(queryResultHandler);
        WebSocket.registerHandle(disconnectHandler);

        $scope.isEmptyResponse = function (tab) {
            return (!isQueryForLeftPanel && tab.result.length === 0);
        };

        $scope.isInvalidQuery = function (tab) {
            return tab.error.length > 0;
        };

    }]);
