import {Badge, Button, Table} from "react-bootstrap";
import React from "react";

export function Territories({territories, onSelect}) {
  const result = []
  territories.forEach(territory => {
    const apps = new Map()
    territory.userConfigurations.map(userConfiguration => {
      const role = userConfiguration.role
      role.applications.map(application => {
        if (apps.has(application.id)) {
          apps.get(application.id).push(role)
        } else {
          apps.set(application.id, [application, role])
        }
      })
    })
    apps.forEach((value, key) => {
      result.push({territory: territory, application: value.shift(), roles: value})
    })
  })

  return (
    <Table>
      <thead>
      <tr>
        <th>Territory</th>
        <th>Role(s)</th>
        <th>Application</th>
      </tr>
      </thead>
      <tbody>
      {
        result.sort((a, b) => a.territory.id - b.territory.id).map(value =>
          <tr key={value.territory.id + '-' + value.application.id}>
            <td><code>{value.territory.name}</code>&nbsp;<Badge variant="info">{value.territory.id}</Badge></td>
            <td>{value.roles.map(role =>
              <React.Fragment key={value.territory.id + '-' + value.application.id + '-2-' + role.id}>
                <code>{role.name}</code> <Badge variant="info">{role.id}</Badge><br/>
              </React.Fragment>
            )}
            </td>
            <td><Button variant="primary" onClick={(e) => {
              onSelect({
                type: "application",
                applicationId: value.application.id,
                territoryId: value.territory.id,
                text: value.application.title + " (" + value.territory.name + ")"
              })
            }}>{value.application.title}</Button></td>
          </tr>
        )
      }
      </tbody>
    </Table>
  );
}