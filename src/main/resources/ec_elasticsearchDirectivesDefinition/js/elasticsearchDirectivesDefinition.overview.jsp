<%@page contentType="text/javascript" %>
    <%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
    <%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

    (function() {
        'use strict';
        var mongoGeneralStatistics = function($log, contextualData, dcTemplateResolver) {

            var directive = {
                restrict        : 'E',
                templateUrl: function(el, attrs) {
                    return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'overview');
                },
                controller      : ESGeneralStatisticsController,
                controllerAs    : 'egsc',
                link            : linkFunc
            };

            return directive;
            function linkFunc(scope, el, attr, ctrls) {}

        };

        angular
            .module('databaseConnector')
            .directive('elasticsearchOverview', ['$log', 'contextualData', 'dcTemplateResolver', mongoGeneralStatistics]);

        function ESGeneralStatisticsController($scope, dcConnectionStatusService, i18n, $filter, $DCSS) {
            var egsc = this;
            egsc.goToConnections = goToConnections;
            egsc.getMessage = i18n.message;
            egsc.$onInit = function() {
                init();
            };

            function init() {
                egsc.title = i18n.format('dc_databaseConnector.label.statistics.databaseOverview', $filter('fLUpperCase')($DCSS.selectedDatabaseType));
                egsc.connectionStatus = dcConnectionStatusService.getCurrentConnectionStatus();
                $scope.$on('connectionStatusUpdate', function(event, connectionStatus) {
                    egsc.connectionStatus = JSON.parse(connectionStatus);
                });
            }

            function goToConnections() {
                $state.go('connections');
            }

        }

        ESGeneralStatisticsController.$inject = ['$scope', 'dcConnectionStatusService', 'i18nService', '$filter', '$DCStateService'];
    })();