/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

KylinApp.service('CubeGraphService', function () {

    var margin = {top: 20, right: 100, bottom: 20, left: 100},
        width = 1100 - margin.right - margin.left,
        height = 600;

    this.buildTree = function (cube) {
        $("#cube_graph_" + cube.name).empty();

        var tree = d3.layout.tree().size([height, width - 160]);
        var diagonal = d3.svg.diagonal().projection(function (d) {
            return [d.y, d.x];
        });

        var svg = d3.select("#cube_graph_" + cube.name).append("svg:svg")
            .attr("width", width + margin.right + margin.left)
            .attr("height", height)
            .append("svg:g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var graphData = {
            "type": "fact",
            "name": cube.detail.fact_table,
            "children": []
        };

        angular.forEach(cube.detail.dimensions, function (dimension, index) {
            if (dimension.join && dimension.join.primary_key.length > 0) {

                var dimensionNode;

                /* Loop through the graphData.children array to find out: If the LKP table is already existed */
                for(var j = 0; j < graphData.children.length; j++ ) {
                    if(graphData.children[j].name == dimension.table){
                        dimensionNode = graphData.children[j];
                        break;
                    }
                }

                /* If not existed, create dimensionNode and push it */
                if(j == graphData.children.length) {
                    dimensionNode = {
                        "type": "dimension",
                        "name": dimension.table,
                        "join": dimension.join,
                        "children": [],
                        "_children": []
                    };
                }

                if (dimension.join && dimension.join.primary_key)
                {
                    angular.forEach(dimension.join.primary_key, function(pk, index){
                        for (var i = 0; i < dimensionNode._children.length; i++) {
                            if(dimensionNode._children[i].name == pk)
                                break;
                        }
                        if(i == dimensionNode._children.length) {
                            dimensionNode._children.push({
                                "type": "column",
                                "name": pk
                            });
                        }

                    });
                }

                if (dimension.derived)
                {
                    angular.forEach(dimension.derived, function(derived, index){
                        for (var i = 0; i < dimensionNode._children.length; i++) {
                            if(dimensionNode._children[i].name == derived)
                                break;
                        }
                        if(i == dimensionNode._children.length) {
                            dimensionNode._children.push({
                                "type": "column",
                                "name": derived + "(DERIVED)"
                            });
                        }
                    });
                }

                if (dimension.hierarchy)
                {
                    angular.forEach(dimension.hierarchy, function(hierarchy, index){
                        for (var i = 0; i < dimensionNode._children.length; i++) {
                            if(dimensionNode._children[i].name == hierarchy)
                                break;
                        }
                        if(i == dimensionNode._children.length) {
                            dimensionNode._children.push({
                                "type": "column",
                                "name": hierarchy.column + "(HIERARCHY)"
                            });
                        }
                    });
                }

                if(j == graphData.children.length) {
                    graphData.children.push(dimensionNode);
                }

            }
        });

        cube.graph = (!!cube.graph) ? cube.graph : {};
        cube.graph.columnsCount = 0;
        cube.graph.tree = tree;
        cube.graph.root = graphData;
        cube.graph.svg = svg;
        cube.graph.diagonal = diagonal;
        cube.graph.i = 0;

        cube.graph.root.x0 = height / 2;
        cube.graph.root.y0 = 0;
        update(cube.graph.root, cube);
    }

    function update(source, cube) {
        var duration = 750;

        // Compute the new tree layout.
        var nodes = cube.graph.tree.nodes(cube.graph.root).reverse();

        // Update the nodes
        var node = cube.graph.svg.selectAll("g.node")
            .data(nodes, function (d) {
                return d.id || (d.id = ++cube.graph.i);
            });

        var nodeEnter = node.enter().append("svg:g")
            .attr("class", "node")
            .attr("transform", function (d) {
                return "translate(" + source.y0 + "," + source.x0 + ")";
            });

        // Enter any new nodes at the parent's previous position.
        nodeEnter.append("svg:circle")
            .attr("r", 4.5)
            .style("fill", function (d) {
                switch (d.type) {
                    case 'fact':
                        return '#fff';
                    case 'dimension':
                        return '#B0C4DE';
                    case 'column':
                        return 'black'
                    default:
                        return '#B0C4DE';
                }
            })
            .on("click", function (d) {
                if (d.children) {
                    d._children = d.children;
                    d.children = null;

                    if (d.type == 'dimension') {
                        cube.graph.columnsCount -= d._children.length;
                    }
                } else {
                    d.children = d._children;
                    d._children = null;

                    if (d.type == 'dimension') {
                        cube.graph.columnsCount += d.children.length;
                    }
                }

                var perColumn = 35;
                var newHeight = (((cube.graph.columnsCount * perColumn > height) ? cube.graph.columnsCount * perColumn : height));
                $("#cube_graph_" + cube.name + " svg").height(newHeight);
                cube.graph.tree.size([newHeight, width - 160]);
                update(d, cube);
            });

        nodeEnter.append("svg:text")
            .attr("x", function (d) {
                return -90;
            })
            .attr("y", 3)
            .style("font-size", "14px")
            .text(function (d) {
                if (d.type == "dimension") {
                    var joinTip = "";

                    angular.forEach(d.join.primary_key, function (pk, index) {
                        joinTip += ( cube.graph.root.name + "." + d.join.foreign_key[index] + " = " + d.name + "." + pk + "<br>");
                    });

                    d.tooltip = d3.select("body")
                        .append("div")
                        .style("position", "absolute")
                        .style("z-index", "10")
                        .style("font-size", "11px")
                        .style("visibility", "hidden")
                        .html(joinTip);
                    var joinType = (d.join) ? (d.join.type) : '';

                    return joinType + " join";
                }
                else {
                    return "";
                }
            })
            .on('mouseover', function (d) {
                return d.tooltip.style("visibility", "visible");
            })
            .on("mousemove", function (d) {
                return d.tooltip.style("top", (event.pageY + 30) + "px").style("left", (event.pageX - 50) + "px");
            })
            .on('mouseout', function (d) {
                return d.tooltip.style("visibility", "hidden");
            });

        nodeEnter.append("svg:text")
            .attr("x", function (d) {
                return 8;
            })
            .attr("y", 3)
            .text(function (d) {
                var dataType = (d.dataType) ? ('(' + d.dataType + ')') : '';

                return d.name + dataType;
            });

        // Transition nodes to their new position.
        nodeEnter.transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            })
            .style("opacity", 1)
            .select("circle")
            .style("fill", function (d) {
                switch (d.type) {
                    case 'fact':
                        return '#fff';
                    case 'dimension':
                        return '#B0C4DE';
                    case 'column':
                        return 'black'
                    default:
                        return '#B0C4DE';
                }
            });

        node.transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + d.y + "," + d.x + ")";
            })
            .style("opacity", 1);

        node.exit().transition()
            .duration(duration)
            .attr("transform", function (d) {
                return "translate(" + source.y + "," + source.x + ")";
            })
            .style("opacity", 1e-6)
            .remove();

        // Update the links…
        var link = cube.graph.svg.selectAll("path.link")
            .data(cube.graph.tree.links(nodes), function (d) {
                return d.target.id;
            });

        // Enter any new links at the parent's previous position.
        link.enter().insert("svg:path", "g")
            .attr("class", "link")
            .attr("d", function (d) {
                var o = {x: source.x0, y: source.y0};
                return cube.graph.diagonal({source: o, target: o});
            })
            .transition()
            .duration(duration)
            .attr("d", cube.graph.diagonal);

        // Transition links to their new position.
        link.transition()
            .duration(duration)
            .attr("d", cube.graph.diagonal);

        // Transition exiting nodes to the parent's new position.
        link.exit().transition()
            .duration(duration)
            .attr("d", function (d) {
                var o = {x: source.x, y: source.y};
                return cube.graph.diagonal({source: o, target: o});
            })
            .remove();

        // Stash the old positions for transition.
        nodes.forEach(function (d) {
            d.x0 = d.x;
            d.y0 = d.y;
        });
    }

});
