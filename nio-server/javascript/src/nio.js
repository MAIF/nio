import 'es6-shim';
import 'whatwg-fetch';
import Symbol from 'es-symbol';

import React from 'react';
import ReactDOM from 'react-dom';

import $ from 'jquery';

import {RoutedNioApp} from "./nio/index";

import 'react-select/dist/react-select.css';

import './index.css';
import {NioTenantAppWithRouter} from "./nio/selectTenantIndex";

if (!window.Symbol) {
  window.Symbol = Symbol;
}
window.$ = $;
window.jQuery = $;

require('bootstrap/dist/js/bootstrap.min');

export function init(node, tenant, logoutUrl, userEmail, securityDefault, securityAuth0) {
  ReactDOM.render(<RoutedNioApp tenant={tenant} logoutUrl={logoutUrl} userEmail={userEmail} securityDefault={securityDefault} securityAuth0={securityAuth0}/>, node)
}

export function initTenant(node, logoutUrl, userEmail, securityDefault, securityAuth0) {
  ReactDOM.render(<NioTenantAppWithRouter logoutUrl={logoutUrl} userEmail={userEmail} securityDefault={securityDefault} securityAuth0={securityAuth0}/>, node)
}
