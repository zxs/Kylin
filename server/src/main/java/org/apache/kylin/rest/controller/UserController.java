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

package org.apache.kylin.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.apache.kylin.rest.service.UserService;

/**
 * Handle user authentication request to protected kylin rest resources by
 * spring security.
 * 
 * @author xduo
 * 
 */
@Controller
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping(value = "/authentication", method = RequestMethod.POST, produces = "application/json")
    public UserDetails authenticate() {
        return authenticatedUser();
    }

    @RequestMapping(value = "/authentication", method = RequestMethod.GET, produces = "application/json")
    public UserDetails authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return null;
        }

        return (UserDetails) authentication.getPrincipal();
    }

    @RequestMapping(value = "/authentication/authorities", method = RequestMethod.GET, produces = "application/json")
    public List<String> getAuthorities() {
        return userService.getUserAuthorities();
    }

}
