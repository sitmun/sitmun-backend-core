import React, {useEffect, useState} from "react";

import {Container, Tab, Tabs} from "react-bootstrap";
import {Config} from "./Config";
import {Territories} from "./Territories";

export function Workspace({token, onSelect}) {

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
      fetch("/api/workspace", requestOptions)
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
    fetchData()
    return () => {
      isCancelled = true;
    }
  }, [token])

  if (isLoading) {
    return <Container>
      <div>No Workspace yet...</div>
    </Container>
  }
  return (
    <Container className={"mt-4"}>
      <Tabs defaultActiveKey="uc">
        <Tab eventKey="uc" title="User Configuration">
          <Territories territories={workspace.territories} onSelect={onSelect}/>
        </Tab>
        <Tab eventKey="props" title="Properties">
          <Config config={workspace.config}/>
        </Tab>
      </Tabs>
    </Container>
  );
}