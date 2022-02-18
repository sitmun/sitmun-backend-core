import React, {useEffect, useState} from "react";

import {Container, Tab, Tabs} from "react-bootstrap";
import {Config} from "./Config";
import {Territory} from "./Territory";
import {Application} from "./Application";
import {Role} from "./Role";
import {Tasks} from "./Tasks";
import {Permissions} from "./Permissions";
import {Cartographies} from "./Cartographies";
import {Tree} from "./Tree";

export function WorkspaceApplication({token, applicationId, territoryId}) {

  const target = "api/workspace/application/" + applicationId + "/territory/" + territoryId
  const [workspace, setWorkspace] = useState()
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {

    let isCancelled = false;
    const fetchData = async () => {
      const requestOptions = {}
      if (token != null) {
        requestOptions['headers'] = new Headers({
          'Authorization': 'Bearer ' + token
        })
      }
      fetch(target, requestOptions)
        .then(res => res.json())
        .then(
          (response) => {
            if (!isCancelled) {
              setWorkspace(response)
              setIsLoading(false)
            }
          },
          (error) => {
            if (!isCancelled) {
              setIsLoading(true)
            }
          }
        );
    };

    fetchData();

    return () => {
      isCancelled = true;
    }
  }, [token]);

  if (isLoading) {
    return <Container>
      <div>No Workspace Application yet...</div>
    </Container>
  }
  return (
    <Container className={"mt-4"}>
      <Tabs defaultActiveKey="territory">
        <Tab eventKey="territory" title="Territory">
          <Territory territory={workspace.territory}/>
        </Tab>
        <Tab eventKey="application" title="Application">
          <Application application={workspace.application}/>
        </Tab>
        {
          workspace.roles.map(role =>
            <Tab key={role.id} eventKey={role.id} title={"Role: " + role.name}>
              <Tabs defaultActiveKey="role">
                <Tab eventKey="role" title="Role">
                  <Role role={role}/>
                </Tab>
                <Tab eventKey="tasks" title="Task">
                  <Tasks tasks={role.tasks}/>
                </Tab>
                <Tab eventKey="permissions" title="Permissions">
                  <Permissions permissions={role.permissions}/>
                </Tab>
                <Tab eventKey="cartographies" title="Cartographies">
                  <Cartographies permissions={role.permissions}/>
                </Tab>
                {
                  role.trees.map(tree =>
                    <Tab key={"tree" + tree.id} eventKey={"tree" + tree.id} title={"Tree: " + tree.name}>
                      <Tree nodes={tree.allNodes} permissions={role.permissions}/>
                    </Tab>
                  )
                }
              </Tabs>
            </Tab>
          )
        }
        <Tab eventKey="props" title="Properties">
          <Config config={workspace.config}/>
        </Tab>
      </Tabs>
    </Container>
  );
}