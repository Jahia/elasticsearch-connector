<dc-spinner spinner-mode="cecc.spinnerOptions.mode" show="cecc.spinnerOptions.showSpinner"></dc-spinner>
<md-content layout-padding>
    <span ng-show="elasticsearchAdvancedForm.$invalid"
          message-key="ec_elasticsearchConnector.label.needPassword"
          style="color: crimson; font-size: 10pt"></span>
    <md-tabs md-dynamic-height md-border-bottom>
        <md-tab label="{{cecc.tabLabels.settings}}">
            <form name="elasticsearchForm">
                <md-content class="md-padding">
                    <md-checkbox ng-model="cecc.connection.isConnected" aria-label="Automatic startup after Creation"
                                 ng-true-value="true" ng-false-value="false"
                                 class="md-warn md-align-top-left" flex>
                        <span class="ipsum" message-key="dc_databaseConnector.label.connection.enableConnection"></span>
                    </md-checkbox>
                    <md-input-container class="md-block" flex-gt-sm="1">
                        <label message-key="ec_elasticsearchConnector.label.host"></label>
                        <input title="host" required ng-minlength="4" name="host"
                               ng-model="cecc.connection.host">
                        <div ng-messages="elasticsearchForm.host.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.host"
                                 ng-class="{'showErrorMessages': elasticsearchForm.host.$invalid && elasticsearchForm.host.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.port"></label>
                        <input title="port" md-maxlength="5" ng-pattern="/[0-9]*/" name="port"
                               ng-model="cecc.connection.port" length-parser length-parser-max="5" range-parser
                               range-max-value="65535">
                        <div ng-messages="elasticsearchForm.port.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.port"
                                 ng-class="{'showErrorMessages': elasticsearchForm.port.$invalid && elasticsearchForm.port.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.id"></label>
                        <input title="Connection Name" md-maxlength="30" name="id" required
                               ng-readonly="cecc.mode === 'edit'"
                               ng-pattern="/^[\w]+[\w\-]+[\w]+$/"
                               ng-model="cecc.connection.id" original-value="{{cecc.connection.oldId}}"
                               connection-id-validator="{{cecc.databaseType}}">
                        <div ng-messages="elasticsearchForm.id.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.id"
                                 ng-class="{'showErrorMessages': elasticsearchForm.id.$invalid && elasticsearchForm.id.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                </md-content>
            </form>
        </md-tab>

        <md-tab label="{{cecc.tabLabels.advancedSettings}}">
            <md-content class="md-padding">
                <form name="elasticsearchAdvancedForm">
                    <!-- ***START*  XPACK SECURITY OPTIONS -->
                    <md-subheader class=" md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.useSecurity"></span>
                        <md-checkbox class="float-right"
                                     ng-model="cecc.connection.options.useXPackSecurity"
                                     ng-true-value="true"
                                     ng-false-value="false"
                                     ng-change="cecc.updateXPackSecurity()">
                        </md-checkbox>
                    </md-subheader>
                    <md-switch ng-if="cecc.connection.options.useXPackSecurity"
                               ng-model="cecc.connection.options.useEncryption"
                               md-invert
                               aria-label="Use encryption">
                        <label style="color:rgba(0,0,0,0.54);">{{cecc.getEncryptionMessage()}}</label>
                    </md-switch>
                    <md-input-container class="md-block col-md-offset-2"
                                        ng-if="cecc.connection.options.useXPackSecurity">
                        <label message-key="ec_elasticsearchConnector.label.user"></label>
                        <input title="User" name="user" required ng-minlength="4" md-maxlength="30"
                               ng-model="cecc.connection.user"
                               ng-pattern="/^[a-zA-Z0-9_-]*$/"
                               ng-change="cecc.updateIsEmpty('user');">
                        <div ng-messages="elasticsearchAdvancedForm.user.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.user"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.user.$invalid && elasticsearchAdvancedForm.user.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block col-md-offset-2"
                                        ng-if="!cecc.isEmpty.user && cecc.connection.options.useXPackSecurity">
                        <label message-key="ec_elasticsearchConnector.label.password"></label>
                        <input title="Password" type="password" name="password" ng-model="cecc.connection.password"
                               required ng-minlength="4" md-maxlength="30"
                               ng-change="cecc.updateIsEmpty('password');">
                        <div ng-messages="elasticsearchAdvancedForm.password.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.password"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.password.$invalid && elasticsearchAdvancedForm.password.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <!-- ***END*  XPACK SECURITY OPTIONS -->
                    <%--<md-divider></md-divider>--%>
                    <!-- ***START*  TRANSPORT OPTIONS -->
                    <md-subheader class="md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.networkSettings"></span>
                    </md-subheader>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.nodesSnifferInterval"></label>
                        <input title="Nodes Sampler Interval" name="nodesSnifferInterval"
                               ng-pattern="/^[0-9]+[s|m]{1}$/"
                               ng-model="cecc.connection.options.nodesSnifferInterval">
                        <md-icon class="md-secondary"
                                 title="Sniffer documentation">
                            <a target="_blank"
                               href="https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/_usage.html">
                                <i class="material-icons">help_outline</i>
                            </a>
                        </md-icon>
                        <div ng-messages="elasticsearchAdvancedForm.nodesSnifferInterval.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.nodesSnifferInterval"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.nodesSnifferInterval.$invalid && elasticsearchAdvancedForm.nodesSnifferInterval.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <!-- ***END*  TRANSPORT OPTIONS -->
                    <%--                    <md-divider></md-divider>--%>

                    <!-- ***START*  ADDITIONAL HOST ADDRESSES OPTION -->
                    <md-subheader class=" md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.additionalHostAddresses"></span>
                        <md-checkbox class="float-right"
                                     ng-model="cecc.enableAdditionalHostAddresses"
                                     ng-true-value="true"
                                     ng-false-value="false"
                                     ng-change="cecc.updateHostAddressesOptions()">
                        </md-checkbox>
                    </md-subheader>
                    <div ng-if="cecc.enableAdditionalHostAddresses">
                        <md-list>
                            <md-list-item
                                    ng-repeat="hostAddress in cecc.connection.options.additionalHostAddresses track by $index "
                                    layout-margin layout-padding layout-fill layout-wrap="" class="box-wrap2">
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="ec_elasticsearchConnector.label.host"></label>
                                    <input title="hostAddressHost" class="md-primary" required
                                           name="hostAddressHost_{{$index}}"
                                           ng-model="hostAddress.host">
                                    <div ng-messages="elasticsearchAdvancedForm['hostAddressHost_' + $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.hostAddress.host"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['hostAddressHost_' + $index].$invalid && elasticsearchAdvancedForm['hostAddressHost_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                :
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="dc_databaseConnector.label.port"></label>
                                    <input title="hostAddressPort" class="md-primary"
                                           name="hostAddressPort_{{$index}}"
                                           ng-pattern="/^[0-9]*$/"
                                           ng-model="hostAddress.port " length-parser length-parser-max="5" range-parser
                                           range-max-value="65535">
                                    <div ng-messages="elasticsearchAdvancedForm['hostAddressPort_' + $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.hostAddress.port"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['hostAddressPort_' + $index].$invalid && elasticsearchAdvancedForm['hostAddressPort_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                <md-icon class="md-secondary"
                                         title="Remove Host Address"
                                         ng-click="cecc.removeHostAddress($index)">
                                    <i class="material-icons">delete</i>
                                </md-icon>
                            </md-list-item>
                            <md-list-item>
                                <md-icon class="md-secondary"
                                         title="Add Host Address"
                                         ng-click="cecc.addHostAddress()">
                                    <i class="material-icons">add</i>
                                </md-icon>
                            </md-list-item>
                        </md-list>
                    </div>
                    <!-- ***END*  ADDITIONAL HOST ADDRESSES OPTION -->
                </form>
            </md-content>
        </md-tab>
    </md-tabs>
    <md-button ng-if="cecc.mode=='create'"
               class="md-raised md-primary float-right"
               ng-click="cecc.createElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="dc_databaseConnector.label.create">
    </md-button>
    <md-button ng-if="cecc.mode=='edit'"
               class="md-raised md-primary float-right"
               ng-click="cecc.editElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="dc_databaseConnector.label.update">
    </md-button>
    <md-button class="md-raised md-primary float-right"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               ng-if="cecc.mode=='import-edit'"
               ng-click="cecc.updateImportedConnection()"
               message-key="dc_databaseConnector.label.ok">
    </md-button>
    <md-button class="md-raised md-primary float-right center-content"
               ng-click="cecc.testElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid">
        <span message-key="ec_elasticsearchConnector.label.test"></span>
        <md-tooltip class="noToolTipAnimation"
                    md-direction="top"
                    md-delay="500">
            <span message-key="dc_databaseConnector.label.connection.testConnection"></span>
        </md-tooltip>
    </md-button>
    <md-button class="float-right"
               ng-click="cecc.cancel()">
        <span ng-if="cecc.mode=='edit' || cecc.mode=='create'"
              message-key="dc_databaseConnector.label.back"></span>
        <span ng-if="cecc.mode=='import-edit'"
              message-key="dc_databaseConnector.label.cancel"></span>
    </md-button>
</md-content>
