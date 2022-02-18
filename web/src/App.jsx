import React from "react";
import {Col, Container, Nav, Navbar, Row} from "react-bootstrap";
import * as ReactDOM from "react-dom";
import 'regenerator-runtime/runtime'

import {Login} from "./Login";
import {Workspace} from './Workspace';
import {WorkspaceApplication} from "./WorkspaceApplication";

import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

function App() {
  const [token, setToken] = React.useState()
  const [selected, setSelected] = React.useState({type: "workspace"})

  function handleUser(token) {
    setToken(token)
    setSelected({type: "workspace"})
  }

  function handleSelected(data) {
    setSelected(data)
  }

  function ShowWorkspace({active}) {
    if (active.type === "workspace")
      return <>
        <Workspace token={token} onSelect={handleSelected}/>
      </>;
    else return <></>;
  }

  function ShowApplicationWorkspace({active}) {
    if (active.type === "application")
      return <>
        <WorkspaceApplication token={token}
                              applicationId={active.applicationId}
                              territoryId={active.territoryId}/>
      </>;
    else return <></>;
  }

  function LinkToApplication() {
    if ("text" in selected)
      return (<>
        <Nav.Link
          href="#application"
          onClick={(e) => setSelected({...selected, type: "application"})}
        >{selected.text}</Nav.Link>
      </>)
    else
      return (<>
      </>);
  }

  return (
    <Container fluid>
      <Row>
        <Col>
          <Navbar bg="light" expand="lg">
            <Navbar.Brand href="#home">SITMUN Workspace</Navbar.Brand>
            <Navbar.Toggle aria-controls="basic-navbar-nav"/>
            <Navbar.Collapse id="basic-navbar-nav">
              <Nav className="mr-auto">
                <Nav.Link
                  href="#workspace"
                  onClick={(e) => setSelected({...selected, type: "workspace"})}
                >Workspace</Nav.Link>
                <LinkToApplication/>
              </Nav>
            </Navbar.Collapse>
            <Navbar.Collapse className="justify-content-end">
              <Navbar.Text>
                Signed in as: <Login updateToken={handleUser}/>
              </Navbar.Text>
            </Navbar.Collapse>
          </Navbar>
        </Col>
      </Row>
      <Row>
        <Col md={12}>
          <ShowWorkspace active={selected}/>
          <ShowApplicationWorkspace active={selected}/>
        </Col>
      </Row>
    </Container>
  );
}

ReactDOM.render(
  <App/>
  ,
  document.getElementById('root')
);