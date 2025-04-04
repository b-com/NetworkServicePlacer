/**
 * ===============================================================================
 * This file is part of Network Service Placer.
 *
 * Copyright 2021-2022 b<>com. All rights reserved.
 *
 * Network Service Placer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Network Service Placer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Network Service Placer. If not, see <https://www.gnu.org/licenses/>.
 * ===============================================================================
 */

var HTTP_STATUS_CODES = {
    '200' : 'OK',
    '201' : 'Created',
    '202' : 'Accepted',
    '203' : 'Non-Authoritative Information',
    '204' : 'No Content',
    '205' : 'Reset Content',
    '206' : 'Partial Content',
    '300' : 'Multiple Choices',
    '301' : 'Moved Permanently',
    '302' : 'Found',
    '303' : 'See Other',
    '304' : 'Not Modified',
    '305' : 'Use Proxy',
    '307' : 'Temporary Redirect',
    '400' : 'Bad Request',
    '401' : 'Unauthorized',
    '402' : 'Payment Required',
    '403' : 'Forbidden',
    '404' : 'Not Found',
    '405' : 'Method Not Allowed',
    '406' : 'Not Acceptable',
    '407' : 'Proxy Authentication Required',
    '408' : 'Request Timeout',
    '409' : 'Conflict',
    '410' : 'Gone',
    '411' : 'Length Required',
    '412' : 'Precondition Failed',
    '413' : 'Request Entity Too Large',
    '414' : 'Request-URI Too Long',
    '415' : 'Unsupported Media Type',
    '416' : 'Requested Range Not Satisfiable',
    '417' : 'Expectation Failed',
    '500' : 'Internal Server Error',
    '501' : 'Not Implemented',
    '502' : 'Bad Gateway',
    '503' : 'Service Unavailable',
    '504' : 'Gateway Timeout',
    '505' : 'HTTP Version Not Supported'
};

var fadeDelay = 500;

$.ajaxSetup({
    contentType: "application/json; charset=utf-8"
});

function load(addr) {
    if(hasParent()) {
        window.parent.loadInParent(addr);
    } else {
        window.location.href = "/index.html";
    }
}

function copyClipboard(data, msg) {
    navigator.clipboard.writeText(data)
        .then(() => { notify("primary", msg, 2000); })
        .catch((error) => { notify("warning", "Unsuccessful copy to clipboard!", 2000); });
}

function notify(status, msg, time) {
    if(hasParent()) {
        window.parent.notifyInParent(status, msg, time);
    } else {
        UIkit.notification({message: msg, status: status, pos: 'bottom-center', timeout: time})
    }
}

function onerror(data) {
    var text = HTTP_STATUS_CODES[data.status];
    UIkit.notification({message: text, status: 'warning', pos: 'bottom-center', timeout: 1000})
}

function hasParent() {
    return window.parent.loadInParent;
}