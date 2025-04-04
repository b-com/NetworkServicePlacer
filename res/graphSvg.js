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

var selectedElem, initialPoint, elemInitialPoint;
function getPoint(evt) {
    var svg = document.getElementById("canvas");
    var ctm = svg.getScreenCTM();
    return {x: (evt.clientX - ctm.e) / ctm.a, y: (evt.clientY - ctm.f) / ctm.d};
}
function startDrag(evt) {
    if (evt.target.classList.contains('GraphNode')) {
        selectedElem = evt.target;
        elemInitialPoint = {x: parseInt(selectedElem.getAttribute("cx")), y: parseInt(selectedElem.getAttribute("cy"))};
        initialPoint = getPoint(evt);
    }
}
function calcTheta(x1, y1, x2, y2) {
    return Math.atan(parseFloat("" + (y2 - y1)) / (x2 - x1));
}
function rotate(x, y, cx, cy, theta) {
    var point = {x: 0.0, y: 0.0};
    x -= cx;
    y -= cy;
    point.x = (x * Math.cos(theta) - y * Math.sin(theta)) + cx;
    point.y = (x * Math.sin(theta) + y * Math.cos(theta)) + cy;
    return point;
}
function updateLinksLocation() {
    var links = document.getElementsByClassName("GraphLink");
    for(var i = 0; i < links.length; i++) {
        var link = links[i];
        var split = link.getAttribute("data").split("-");
        var srcNodeElem = document.getElementById("Node-" + split[0]);
        var dstNodeElem = document.getElementById("Node-" + split[1]);
        var linkColorElem = document.getElementById("LinkColor-" + link.getAttribute("id").split("-")[1]);
        var srcPoint = {x: parseInt(srcNodeElem.getAttribute("cx")), y: parseInt(srcNodeElem.getAttribute("cy"))};
        var dstPoint = {x: parseInt(dstNodeElem.getAttribute("cx")), y: parseInt(dstNodeElem.getAttribute("cy"))};
        var theta = calcTheta(srcPoint.x, srcPoint.y, dstPoint.x, dstPoint.y);
        var sep = (dstPoint.x < srcPoint.x);
        var deltaRadius = 25.0;
        var deltaAngle = Math.PI / 12.0;
        var r1 = rotate(srcPoint.x + deltaRadius, srcPoint.y, srcPoint.x, srcPoint.y, theta + (sep ? (Math.PI + deltaAngle) : deltaAngle));
        var r2 = rotate(dstPoint.x + deltaRadius, dstPoint.y, dstPoint.x, dstPoint.y, theta + (sep ? (-deltaAngle) : (Math.PI - deltaAngle)));
        link.setAttribute("x1", "" + parseInt("" + r1.x));
        link.setAttribute("y1", "" + parseInt("" + r1.y));
        link.setAttribute("x2", "" + parseInt("" + r2.x));
        link.setAttribute("y2", "" + parseInt("" + r2.y));
        linkColorElem.setAttribute("x1", "" + parseInt("" + r1.x));
        linkColorElem.setAttribute("y1", "" + parseInt("" + r1.y));
        linkColorElem.setAttribute("x2", "" + parseInt("" + r2.x));
        linkColorElem.setAttribute("y2", "" + parseInt("" + r2.y));
    }
}
function drag(evt) {
    if (selectedElem) {
        evt.preventDefault();
        var mousePoint = getPoint(evt);
        var delta = {x: mousePoint.x - initialPoint.x, y: mousePoint.y - initialPoint.y};
        var newPoint = {x: elemInitialPoint.x + delta.x, y: elemInitialPoint.y + delta.y};
        selectedElem.setAttribute("cx", "" + newPoint.x);
        selectedElem.setAttribute("cy", "" + newPoint.y);

        var nodeIndex = selectedElem.id.split("-")[1];

        var nodeLabel = document.getElementById("NodeLabel-" + nodeIndex);
        nodeLabel.setAttribute("x", "" + newPoint.x);
        nodeLabel.setAttribute("y", "" + newPoint.y);

        updateLinksLocation();
    }
}
function endDrag(evt) {
    selectedElem = false;
}
function createNode(node, links) {
    var svg = document.getElementById("canvas");
    var nodeElem = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    nodeElem.setAttribute("id", "Node-" + node.label);
    nodeElem.classList.add("GraphNode");
    nodeElem.setAttribute("fill", "black");
    nodeElem.setAttribute("cx", "" + node.x);
    nodeElem.setAttribute("cy", "" + node.y);
    nodeElem.setAttribute("r", "25");
    //nodeElem.setAttribute("opacity", "0.5");

    var title = document.createElementNS("http://www.w3.org/2000/svg", "title");
    title.setAttribute("id", "NodeTitle-" + node.label);
    var toolTip = "";
    toolTip += "Input links: " + "\n";
    var loopLink = "";
    for(var i = 0; i < links.length; i++) {
        if((links[i].dstNode === links[i].srcNode) && (links[i].dstNode === node.label)) {
            loopLink = links[i].label;
            continue;
        }
        if(links[i].dstNode === node.label) {
            toolTip += "  Link " + links[i].label + " (from node "+ links[i].srcNode + ")" + "\n";
        }
    }
    toolTip += "Output links: " + "\n";
    for(var i = 0; i < links.length; i++) {
        if(links[i].dstNode === links[i].srcNode) {
            continue;
        }
        if(links[i].srcNode === node.label) {
            toolTip += "  Link " + links[i].label + " (to node "+ links[i].dstNode + ")" + "\n";
        }
    }
    toolTip += "Loop back link: " + loopLink + "\n";
    title.innerHTML = toolTip;

    nodeElem.appendChild(title);

    svg.appendChild(nodeElem);

    var nodeLabelElem = document.createElementNS("http://www.w3.org/2000/svg", "text");
    nodeLabelElem.setAttribute("id", "NodeLabel-" + node.label);
    nodeLabelElem.classList.add("GraphNodeLabel");
    nodeLabelElem.setAttribute("fill", "white");
    nodeLabelElem.setAttribute("x", "" + node.x);
    nodeLabelElem.setAttribute("y", "" + node.y);
    nodeLabelElem.setAttribute("text-anchor", "middle");
    nodeLabelElem.setAttribute("font-size", "1.8em");
    nodeLabelElem.setAttribute("alignment-baseline", "middle");
    nodeLabelElem.innerHTML = "" + node.label;
    svg.appendChild(nodeLabelElem);
}
function createlink(link) {
    var svg = document.getElementById("canvas");
    var linkColorElem = document.createElementNS("http://www.w3.org/2000/svg", "linearGradient");
    linkColorElem.setAttribute("id", "LinkColor-" + link.label);
    linkColorElem.setAttribute("gradientUnits", "userSpaceOnUse");
    svg.insertBefore(linkColorElem, svg.firstChild);

    var linkColorStop = document.createElementNS("http://www.w3.org/2000/svg", "stop");
    linkColorStop.setAttribute("offset", "0");
    linkColorStop.setAttribute("stop-color", "#EFEFEF");
    linkColorElem.appendChild(linkColorStop);
    var linkColorStop = document.createElementNS("http://www.w3.org/2000/svg", "stop");
    linkColorStop.setAttribute("offset", "1");
    linkColorStop.setAttribute("stop-color", "black");
    linkColorElem.appendChild(linkColorStop);

    var linkElem = document.createElementNS("http://www.w3.org/2000/svg", "line");
    linkElem.setAttribute("id", "Link-" + link.label);
    linkElem.setAttribute("data", "" + link.srcNode + "-" + link.dstNode);
    linkElem.classList.add("GraphLink");
    linkElem.setAttribute("stroke-width", "2");
    linkElem.setAttribute("stroke", "url(#LinkColor-" + link.label + ")");
    linkElem.setAttribute("marker-end", "url(#arrowhead)");

    svg.insertBefore(linkElem, svg.firstChild);
}
$("#canvas").css("opacity","0");
var graphLoaded = false;
function loadGraph(command, name) {
    $("#canvas").empty();
    $.ajax({
        url: "/api/file/" + command + "/" + name,
        type: "GET",
        success: function (data, status) {
            $("#canvas").empty();
            var svg = document.getElementById("canvas");
            var defs = document.createElementNS("http://www.w3.org/2000/svg", "defs");
            var marker = document.createElementNS("http://www.w3.org/2000/svg", "marker");
            marker.setAttribute("id", "arrowhead");
            marker.setAttribute("markerWidth", "8");
            marker.setAttribute("markerHeight", "8");
            marker.setAttribute("refX", "8");
            marker.setAttribute("refY", "4");
            marker.setAttribute("orient", "auto");
            var polygon = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
            polygon.setAttribute("points", "0 0, 8 4, 0 8");
            marker.appendChild(polygon);
            defs.appendChild(marker);
            svg.appendChild(defs);

            $("#canvas").css("opacity","1");
            var nodes = data.nodes;
            var links = data.links;
            for(var i = 0; i < nodes.length; i++) {
                createNode(nodes[i], links);
            }

            for(var i = 0; i < links.length; i++) {
                if(links[i].loop == false) {
                    createlink(links[i]);
                }
            }
            updateLinksLocation();
            graphLoaded = true;
        }
    });
}
function configureMouse() {
    var svg = document.getElementById("canvas");
    svg.addEventListener('mousedown', startDrag);
    svg.addEventListener('mousemove', drag);
    svg.addEventListener('mouseup', endDrag);
    svg.addEventListener('mouseleave', endDrag);
}