<%@page contentType="text/javascript" %>
    <%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
    <%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

    (function () {
        'use strict';
        var elasticsearchConnectionDirective = function ($log, contextualData, dcTemplateResolver) {
            var directive = {
                restrict: 'E',
                templateUrl: function (el, attrs) {
                    return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'connectionManagement');
                },
                controller: ElasticsearchConnectionDirectiveController,
                controllerAs: 'cecc',
                bindToController: {
                    mode: '@',
                    connection: '=',
                    databaseType: '@',
                    closeDialog: '&'
                },
                link: linkFunc
            };

            return directive;

            function linkFunc(scope, el, attr, ctrls) {
            }

        };

        angular
            .module('databaseConnector')
            .directive('elasticsearchConnectionDirective', ['$log', 'contextualData', 'dcTemplateResolver', elasticsearchConnectionDirective]);

        var ElasticsearchConnectionDirectiveController = function ($scope, contextualData,
                                                                   dcDataFactory, $mdToast, i18n, $DCSS) {
            const DEFAULT_HTTP_PORT = 9200;

            var cecc = this;
            cecc.isEmpty = {};
            cecc.spinnerOptions = {
                showSpinner: false,
                mode: 'indeterminate'
            };
            cecc.tabLabels = {
                settings: i18n.message('dc_databaseConnector.label.settings'),
                advancedSettings: i18n.message('dc_databaseConnector.label.advancedSettings')
            };
            cecc.validations = {
                host: {
                    'required': i18n.message('dc_databaseConnector.label.validation.required'),
                    'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '15'),
                    'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4')
                },
                port: {
                    'pattern': i18n.format('dc_databaseConnector.label.validation.range', '1|65535')
                },
                id: {
                    'required': i18n.message('dc_databaseConnector.label.validation.required'),
                    'connection-id-validator': i18n.message('dc_databaseConnector.label.validation.connectionIdInUse'),
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.connectionId'),
                    'md-maxlength': i18n.format('dc_databaseConnector.label.validation.minLength', '30')
                },
                user: {
                    'required': i18n.message('dc_databaseConnector.label.validation.required'),
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.elasticsearch.user'),
                    'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4'),
                    'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '30')
                },
                password: {
                    'required': i18n.message('dc_databaseConnector.label.validation.required'),
                    'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4'),
                    'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '30')
                },
                hostAddress: {
                    host: {
                        'required': i18n.message('dc_databaseConnector.label.validation.required')
                    },
                    port: {
                        'pattern': i18n.format('dc_databaseConnector.label.validation.range', '1|65535')
                    }
                },
                pingTimeout: {
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.withUnit')
                },
                nodesSnifferInterval: {
                    'pattern': i18n.message('ec_elasticsearchConnector.label.validation.withUnit')
                }
            };

            cecc.createElasticSearchConnection = createElasticSearchConnection;
            cecc.editElasticSearchConnection = editElasticSearchConnection;
            cecc.testElasticSearchConnection = testElasticSearchConnection;
            cecc.cancel = cancel;
            cecc.updateIsEmpty = updateIsEmpty;
            cecc.updateImportedConnection = updateImportedConnection;
            cecc.initAdditionalHostAddresses = initAdditionalHostAddresses;
            cecc.addHostAddress = addHostAddress;
            cecc.removeHostAddress = removeHostAddress;
            cecc.updateHostAddressesOptions = updateHostAddressesOptions;
            cecc.updateXPackSecurity = updateXPackSecurity;
            cecc.getEncryptionMessage = getEncryptionMessage;

            cecc.getMessage = i18n.message;

            cecc.$onInit = function init() {
                if (_.isUndefined(cecc.connection.port) || cecc.connection.port == null) {
                    cecc.connection.port = DEFAULT_HTTP_PORT;
                } else {
                    //Make sure it is treated as a string in order for the max length validation to work correctly.
                    cecc.connection.port += "";
                }
                cecc.isEmpty.password = updateIsEmpty('password');
                cecc.isEmpty.user = updateIsEmpty('user');
                if (cecc.mode === 'import-edit') {
                    cecc.connection.oldId = null;
                }
                if (cecc.mode === 'edit') {
                    cecc.connection.oldId = angular.copy(cecc.connection.id);
                } else {
                    cecc.connection.isConnected = true;
                }
                if (_.isUndefined(cecc.connection.options) || cecc.connection.options == null || _.isString(cecc.connection.options) && cecc.connection.options.trim() == '') {
                    cecc.connection.options = {};
                } else if (_.isString(cecc.connection.options)) {
                    cecc.connection.options = JSON.parse(cecc.connection.options);
                }

                if (cecc.connection.options.additionalHostAddresses) {
                    cecc.enableAdditionalHostAddresses = true;
                }
            };

            function createElasticSearchConnection() {
                if (cecc.mode === 'import-edit') {
                    return;
                }
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/add';

                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);
                if (options == null) {
                    delete data.options;
                } else {
                    data.options = options;
                }

                if (!cecc.connection.options.useXPackSecurity) {
                    data.user = "";
                    data.password = "";
                }

                dcDataFactory.customRequest({
                    url: url,
                    method: 'POST',
                    data: data
                }).then(function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    showConfirmationToast(response.connectionVerified);
                    $scope.closeDialog('hide');
                }, function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    console.error(response);
                    $mdToast.show(
                        $mdToast.simple()
                            .textContent(i18n.message('dc_databaseConnector.toast.title.connectionInvalid'))
                            .position('top right')
                            .toastClass('toast-warn')
                            .hideDelay(3000)
                    );
                });
            }

            function editElasticSearchConnection() {
                if (cecc.mode === 'import-edit') {
                    $scope.closeDialog('hide');
                    return;
                }
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/edit';
                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);
                if (options == null) {
                    delete data.options;
                } else {
                    data.options = options;
                }
                dcDataFactory.customRequest({
                    url: url,
                    method: 'PUT',
                    data: data
                }).then(function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    showConfirmationToast(response.connectionVerified);
                    if (!response.connectionVerified) {
                        cecc.connection.isConnected = false;
                    }
                    $scope.closeDialog('hide');
                }, function (response) {
                    cecc.spinnerOptions.showSpinner = false;
                    cecc.connection.isConnected = false;
                    console.log('error', response);
                    $mdToast.show(
                        $mdToast.simple()
                            .textContent(i18n.message('dc_databaseConnector.toast.title.connectionInvalid'))
                            .position('top right')
                            .toastClass('toast-error')
                            .hideDelay(3000)
                    );
                });
            }

            function testElasticSearchConnection() {
                cecc.spinnerOptions.showSpinner = true;
                var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/testconnection';
                var data = angular.copy(cecc.connection);
                var options = prepareOptions(data.options);

                if (options === null) {
                    delete data.options;
                } else {
                    data.options = options;
                }

                if (data.options == null || !data.options.useXPackSecurity) {
                    data.user = "";
                    data.password = "";
                }
                dcDataFactory.customRequest({
                    url: url,
                    method: 'POST',
                    data: data
                }).then(function (response) {
                    console.log(response);
                    var textContent = response.result ? i18n.message('dc_databaseConnector.toast.title.connectionValid') : i18n.message('dc_databaseConnector.toast.title.connectionInvalid')
                    $mdToast.show(
                        $mdToast.simple()
                            .textContent(textContent)
                            .position('top right')
                            .toastClass(response.result ? 'toast-success' : 'toast-error')
                            .hideDelay(3000)
                    );
                    cecc.spinnerOptions.showSpinner = false;
                }, function (response) {
                    console.error(response);
                    cecc.spinnerOptions.showSpinner = false;
                    $mdToast.show(
                        $mdToast.simple()
                            .textContent(i18n.message('dc_databaseConnector.toast.title.connectionInvalid'))
                            .position('top right')
                            .toastClass('toast-error')
                            .hideDelay(3000)
                    );
                });
            }

            function cancel() {
                $DCSS.selectedDatabaseType = null;
                $scope.closeDialog('cancel');
            }

            function updateIsEmpty(property) {
                //Assigns and returns new value
                return cecc.isEmpty[property] = cecc.connection[property] === undefined || cecc.connection[property] === null || (typeof cecc.connection[property] === 'string' && cecc.connection[property].trim().length === 0);
            }

            function showConfirmationToast(verified) {
                var textContent = verified ? i18n.message('dc_databaseConnector.toast.message.connectionVerificationSuccessful') : i18n.message('dc_databaseConnector.toast.message.connectionVerificationFailed');
                $mdToast.show(
                    $mdToast.simple()
                        .textContent(textContent)
                        .position('top right')
                        .toastClass(verified ? 'toast-success' : 'toast-warn')
                        .hideDelay(3000)
                );
            }

            function prepareOptions(options) {
                //Remove nodes sniffer interval if it has not been set.
                if (options.nodesSnifferInterval == null || (_.isString(options.nodesSnifferInterval) && options.nodesSnifferInterval.trim().length == 0)) {
                    delete options.nodesSnifferInterval
                }

                //Additional Transport Addresses
                if (_.isEmpty(options.additionalHostAddresses)) {
                    delete options.additionalHostAddresses
                }

                return _.isEmpty(options) ? null : options;
            }

            function updateXPackSecurity() {
                if (!cecc.connection.options.useXPackSecurity) {
                    cecc.connection.user = "";
                    cecc.connection.password = "";
                    cecc.connection.options.useEncryption = false;
                }
            }

            function updateImportedConnection() {
                if (!cecc.connection.options.useXPackSecurity) {
                    cecc.connection.user = "";
                    cecc.connection.password = "";
                    cecc.connection.options.useEncryption = false;
                }
                $scope.closeDialog('hide');
            }

            function addHostAddress() {
                if (!_.isUndefined(cecc.connection.options.additionalHostAddresses)) {
                    cecc.connection.options.additionalHostAddresses.push({host: null, port: DEFAULT_HTTP_PORT});
                }
            }

            function removeHostAddress(index) {
                if (!_.isUndefined(cecc.connection.options.additionalHostAddresses)) {
                    cecc.connection.options.additionalHostAddresses.splice(index, 1);
                }
            }

            function updateHostAddressesOptions() {
                if (cecc.enableAdditionalHostAddresses) {
                    if (_.isUndefined(cecc.connection.options.additionalHostAddresses) || cecc.connection.options.additionalHostAddresses == null) {
                        //create a host address object
                        cecc.connection.options.additionalHostAddresses = [{host: null, port: DEFAULT_HTTP_PORT}];
                    }
                } else {
                    //remove host addresses
                    delete cecc.connection.options.additionalHostAddresses
                }
            }

            function initAdditionalHostAddresses() {
                if (!_.isUndefined(cecc.connection.options.additionalHostAddresses) && _.isEmpty(cecc.connection.options.additionalHostAddresses)) {
                    cecc.connection.options.additionalHostAddresses.push({});
                }
            }

            function getEncryptionMessage() {
                return i18n.message(cecc.connection.options.useEncryption ? 'ec_elasticsearchConnector.label.security.encryption.enabled' : 'ec_elasticsearchConnector.label.security.encryption.disabled')
            }
        };

        ElasticsearchConnectionDirectiveController.$inject = ['$scope', 'contextualData',
            'dcDataFactory', '$mdToast', 'i18nService', '$DCStateService'];

        angular
            .module('databaseConnector')
            .run(['$rootScope', '$DCConnectionManagerService', function ($rootScope, $DCCMS) {
                const databaseType = 'ELASTICSEARCH';
                const addVersionToConnectionData = function (connectionData, data) {
                    connectionData.canRetrieveStatus = false;
                    if (data.success) {
                        try {
                            const parseData = JSON.parse(data.success);
                            connectionData.canRetrieveStatus = !_.isEmpty(parseData.status);
                            if (parseData.status.statistics != undefined) {
                                if (parseData.status.statistics.nodes.versions != null) {
                                    connectionData.dbVersion = parseData.status.statistics.nodes.versions[0];
                                }
                                console.log("Removing uptime");
                                if (parseData.status.statistics.timestamp != undefined) {
                                    connectionData.uptime = Math.ceil(parseData.status.statistics.nodes.jvm.max_uptime_in_millis / 1000);
                                }
                            }
                        } catch (e) {
                            console.error('Failed to parse json for success data [' + databaseType + ']');
                        }
                    }
                }
                $DCCMS.registerStatusHandler(databaseType, addVersionToConnectionData);
                $rootScope.$on('destroy', function () {
                    $DCCMS.unregisterStatusHandler(databaseType);
                });
            }]);
    })();
