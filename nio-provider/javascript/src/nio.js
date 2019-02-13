import 'es6-shim';
import 'whatwg-fetch';
import Symbol from 'es-symbol';

import React from 'react';
import ReactDOM from 'react-dom';

import $ from 'jquery';

import {RoutedNioApp} from "./nio/index";

import './index.css';

if (!window.Symbol) {
  window.Symbol = Symbol;
}
window.$ = $;
window.jQuery = $;

require('bootstrap/dist/js/bootstrap.min');

export function init(node, userEmail, webSocketHost) {
  ReactDOM.render(<RoutedNioApp userEmail={userEmail} webSocketHost={webSocketHost}/>, node)
}
