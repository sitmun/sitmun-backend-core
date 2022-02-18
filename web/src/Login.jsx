import React, {useState} from "react";
import {Button, Form, FormControl, InputGroup, Modal} from "react-bootstrap";

export function Login({updateToken}) {

  const [show, setShow] = useState(false);
  const [user, setUser] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [password, setPassword] = useState("");
  const [error, setError] = useState(false);

  function LoginText({user}) {
    if (user == null) {
      return ("Public Citizen");
    } else {
      return (user);
    }
  }

  const handleClose = () => setShow(false);
  const handleShow = () => setShow(true);
  const updateUser = (event) => {
    setError(false)
    setUser(event.target.value);
  }
  const updatePassword = (event) => {
    setError(false)
    setPassword(event.target.value);
  }

  function validateForm() {
    return user != null && user.length > 0 && password.length > 0;
  }

  const handleLogout = (event) => {
    event.preventDefault();
    setCurrentUser(null)
    updateToken(null)
    handleClose()
  }

  const handleLogin = (event) => {
    event.preventDefault();
    const requestOptions = {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({username: user, password: password})
    }
    fetch("/api/authenticate", requestOptions)
      .then(response => {
        if (response.ok) {
          return response.json()
        }
        return Promise.reject(response)
      })
      .then(response => {
        setCurrentUser(user)
        updateToken(response.id_token)
        handleClose()
      })
      .catch(e => setError(true))
  }

  return (
    <>
      <Button variant="primary" onClick={handleShow}>
        <LoginText user={currentUser}/>
      </Button>
      <Modal
        show={show}
        onHide={handleClose}
      >
        <Modal.Header closeButton>
          <Modal.Title>Login</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <InputGroup size={"sm"} className={"mb-3"} hasValidation>
            <FormControl placeholder={"username"} onChange={updateUser} isInvalid={error}/>
          </InputGroup>
          <InputGroup size={"sm"} className={"mb-3"} hasValidation>
            <FormControl placeholder={"password"} onChange={updatePassword} isInvalid={error}/>
            <Form.Control.Feedback type="invalid">
              Invalid username or password.
            </Form.Control.Feedback>
          </InputGroup>
        </Modal.Body>

        <Modal.Footer>
          <Button variant="secondary" disabled={!validateForm()} onClick={handleLogin}>Login</Button>
          <Button variant="primary" onClick={handleLogout}>Logout</Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}

